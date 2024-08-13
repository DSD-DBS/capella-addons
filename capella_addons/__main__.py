# Copyright DB InfraGO AG and contributors
# SPDX-License-Identifier: Apache-2.0
import contextlib
import itertools
import json
import logging
import os
import pathlib
import platform
import shutil
import subprocess
import sys
import tempfile
import threading
import typing as t
from enum import Enum

import click
import lxml.builder
import lxml.etree

import capella_addons

logging.basicConfig(level=logging.INFO, format="%(levelname)s - %(message)s")
logger = logging.getLogger(__name__)
response_stdout_generator = itertools.count(1)
response_stderr_generator = itertools.count(1)


class BuildWorkspaceStatus(Enum):
    """Enumeration of the build workspace status.

    see
    https://github.com/eclipse-jdtls/eclipse.jdt.ls/blob/master/org.eclipse.jdt.ls.core/src/org/eclipse/jdt/ls/core/internal/BuildWorkspaceStatus.java
    """

    FAILED = 0
    SUCCEED = 1
    WITH_ERROR = 2
    CANCELLED = 3
    UNKNOWN_ERROR = 4


AWAITED_RESPONSE_ID = 0

E = lxml.builder.ElementMaker()
MANIFEST_PATH = pathlib.Path("META-INF/MANIFEST.MF")
PLUGIN_XML_PATH = pathlib.Path("plugin.xml")
PATH_BLACKLIST = (
    ".pde.",
    "/jre/",
    "/org.eclipse.equinox.p2.repository/",
    "ant",
    "artifacts.jar",
    "content.jar",
    "ease",
    "egit",
    "jdt.debug",
    "jgit",
    "pydev",
)


@click.group()
@click.option(
    "-v",
    "--verbose",
    is_flag=True,
    default=False,
    help="Enable verbose output.",
    expose_value=False,
    callback=lambda _, __, x: logging.getLogger().setLevel(
        logging.DEBUG if x else logging.INFO
    ),
)
@click.version_option(
    version=capella_addons.__version__,
    prog_name="capella-addons",
    message="%(prog)s %(version)s",
)
def main() -> None:
    """Console tools to develop, build, pack, and deploy Capella addons.

    Preconditions that must be fulfilled on the system:

    The command line tool `mvn` must be installed and accessible via the
    user's `PATH` environment variable.
    `mvn` is the Maven build tool and is used to analyse the
    dependencies listed in the `pom.xml` file to build the `.classpath`
    file for a Capella addon project.

    A Java Development Kit (JDK) must be installed.
    An Eclipse JDT Language Server (JDTLS) must be installed.

    Common workflow:

    1. Change to the directory of the Eclipse/ Capella addon project.
    1. Run the `build-classpath` command to generate the `.classpath`
       file.
    1. Run the `build-workspace` command to build the workspace of the
       Eclipse/ Capella addon project.
    1. Run the `package` command to package the Eclipse/ Capella addon.
       This creates a JAR file in the `target` directory.
    1. Run the `deploy` command to deploy the Eclipse/ Capella addon to
       the target platform.
    """


def _third_party_lib_paths() -> list[pathlib.Path]:
    """Return the paths to the third-party libraries."""
    classpath_root = _read_xml_file(".classpath")
    third_party_lib_paths = classpath_root.xpath(
        'classpathentry[@kind="lib"]/@path'
    )
    return sorted([pathlib.Path(p) for p in third_party_lib_paths])


def compute_jar_name() -> str:
    """Compute and return the name of the jar file to be built."""
    pom = _read_xml_file("pom.xml")
    # get the namespace from the root element
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}  # Register the namespace
    group_id = pom.xpath("//m:groupId", namespaces=ns)
    artifact_id = pom.xpath("//m:artifactId", namespaces=ns)
    version = pom.xpath("//m:version", namespaces=ns)
    group_id = group_id[0].text if group_id else "unknown"
    artifact_id = artifact_id[0].text if artifact_id else "unknown"
    version = version[0].text if version else "unknown"

    # Determine the operating system
    os_name = platform.system().lower()
    if os_name == "darwin":
        os_name = "macos"

    # Determine the architecture
    architecture = platform.machine().lower()
    if architecture in ["x86_64", "amd64"]:
        architecture = "x64"
    elif architecture in ["i386", "i686"]:
        architecture = "x86"
    elif architecture.startswith("arm"):
        architecture = "arm"

    jar_name = (
        f"{group_id}.{artifact_id}_{version}_{os_name}_{architecture}.jar"
    )
    logger.debug("Computed jar name: %s", jar_name)
    return jar_name


def _output_and_jar_path() -> tuple[pathlib.Path, pathlib.Path]:
    """Return paths to output dir and the jar file to be built."""
    classpath_root = _read_xml_file(".classpath")
    output = classpath_root.xpath('//classpathentry[@kind="output"]')
    if not output:
        click.echo(
            "Output directory not found. Missing `classpathentry` with kind "
            "`output` in `.classpath` file."
        )
        sys.exit(1)
    output_path = pathlib.Path(output[0].get("path"))
    if not list(output_path.iterdir()):
        click.echo(f"Output directory `{output_path}` is empty.")
        sys.exit(1)
    jar_name = compute_jar_name()
    jar_path = pathlib.Path("target") / jar_name
    return output_path, jar_path


def _read_xml_file(path: str) -> lxml.etree._ElementTree:
    """Read the classpath file."""
    if not pathlib.Path(path).exists():
        click.echo(f"`File {path}` not found.")
        sys.exit(1)
    return lxml.etree.parse(path)


def _collect_target_platform_plugins(
    target_path: pathlib.Path,
) -> list[lxml.etree._Element]:
    """Add the target platform plugins to the classpath."""
    # Recursively find all src JARs:
    sources: set[pathlib.Path] = set(target_path.glob("**/*.source_*.jar"))
    # Recursively find all lib JARs:
    dropins_jars = list(target_path.glob("dropins/**/*.jar"))
    features_jars = list(target_path.glob("features/**/*.jar"))
    jre_jars = list(target_path.glob("jre/**/*.jar"))
    plugins_jars = list(target_path.glob("plugins/**/*.jar"))
    libs = list(
        set(dropins_jars + features_jars + jre_jars + plugins_jars) - sources
    )
    libs = [lst for lst in libs if lst.name != compute_jar_name()]
    srcs = list(sources)
    target_classpaths = []
    for src in srcs:
        skip = False
        for pattern in PATH_BLACKLIST:
            skip = pattern in str(src)
            if skip:
                break
        if skip:
            continue
        # get parent dir
        parent = src.parent
        # get base name
        base = src.name
        lib = parent / base.replace(".source_", "_")
        with contextlib.suppress(ValueError):
            libs.remove(lib)
        if lib.is_file() and src.is_file():
            target_classpaths.append(
                E.classpathentry(
                    kind="lib", path=str(lib), sourcepath=str(src)
                )
            )
    for lib in libs:
        skip = False
        for pattern in PATH_BLACKLIST:
            skip = pattern in str(lib)
            if skip:
                break
        if skip:
            continue
        if lib.is_file():
            target_classpaths.append(
                E.classpathentry(kind="lib", path=str(lib))
            )
    target_classpaths.sort(key=lambda x: x.get("path", ""))
    return target_classpaths


@main.command()
@click.option(
    "--java-execution-environment",
    type=click.Choice(
        [
            "JavaSE-17",
            "JavaSE-18",
            "JavaSE-19",
            "JavaSE-20",
            "JavaSE-21",
            "JavaSE-22",
        ]
    ),
    required=True,
    help=(
        "The Java execution environment to be used. The value must be an"
        " exact match of the execution environment name as it appears in"
        " the enumeration named `ExecutionEnvironment` as defined here:"
        " https://github.com/eclipse-jdtls/eclipse.jdt.ls/wiki/"
        "Running-the-JAVA-LS-server-from-the-command-line#initialize-request"
    ),
)
@click.argument("filename", type=click.Path(exists=True, dir_okay=True))
@click.argument(
    "target_platform_path", type=click.Path(exists=True, dir_okay=True)
)
def build_classpath(
    java_execution_environment: str,
    filename: pathlib.Path,
    target_platform_path: pathlib.Path,
) -> None:
    """Build `.classpath` file.

    \b
    Arguments
    ---------
    filename
        Any Java project file. The classpath will be built for this
        project.
    target_platform_path
        The installation directory of an Eclipse/ Capella application
        that will be referenced as target platform to build the
        classpath.
    """  # noqa: D301
    target_path = pathlib.Path(target_platform_path)
    if not target_path.is_dir():
        click.echo(
            f"Target platform installation dir `{target_path}` not found."
        )
        sys.exit(1)
    classpaths = [
        E.classpathentry(kind="src", path="src", including="**/*.java"),
        E.classpathentry(kind="output", path="target/classes"),
        E.classpathentry(
            kind="con",
            path=(
                "org.eclipse.jdt.launching.JRE_CONTAINER/"
                "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/"
                f"{java_execution_environment}"
            ),
        ),
    ]
    with tempfile.NamedTemporaryFile(mode="w", delete=False) as w:
        mvn_cmd = [
            "mvn",
            "-q",
            "dependency:build-classpath",
            f"-Dmdep.outputFile={w.name}",
        ]

        def find_eclipse_jdtls_project_directory() -> pathlib.Path | None:
            path = pathlib.Path(filename)
            for parent in path.parents:
                if (parent / ".project").is_file() and (
                    parent / "pom.xml"
                ).is_file():
                    return parent
            return None

        project_dir = find_eclipse_jdtls_project_directory()
        if project_dir is None:
            raise RuntimeError(
                "Could not find a valid Eclipse JDTLS project directory."
                " containing a `.project` and a `pom.xml` file."
            )
        os.chdir(project_dir)
        print(
            f"Building classpath for project in `{project_dir.resolve()}`..."
        )
        # Run command and wait:
        result = subprocess.run(
            mvn_cmd,
            capture_output=True,
            text=True,
            cwd=project_dir,
            check=False,
        )
        if result.returncode != 0:
            raise RuntimeError(result.stdout)
    with open(w.name, encoding="utf-8") as tmp:
        # Replace all colons with newlines and sort the lines:
        classpath_3rdparty = tmp.read().replace(":", "\n").splitlines()
    classpath_3rdparty.sort()
    for path in classpath_3rdparty:
        classpaths.append(E.classpathentry(kind="lib", path=path))
    target_classpaths = _collect_target_platform_plugins(target_path)
    classpath = E.classpath(*(classpaths + target_classpaths))
    tree = lxml.etree.ElementTree(classpath)
    xml_string = lxml.etree.tostring(
        tree, xml_declaration=True, encoding="utf-8", pretty_print=True
    )
    pathlib.Path(".classpath").write_bytes(xml_string)
    print("Created `.classpath` file.")


class LSPClient:
    id_generator = itertools.count(1)

    def __init__(self, process):
        self.awaited_response_id = 0
        self.lock = threading.Lock()
        self.process = process
        self.request_processed_event = threading.Event()
        self.responses: list[dict] = []

    def send_request(self, request):
        method, params, wait_for_response = (
            request["method"],
            request["params"],
            request["wait_for_response"],
        )
        request_id = request.get("id")
        logger.debug("Sending request with id %d", request_id)
        sys.stdout.flush()
        self.request_processed_event.clear()
        with self.lock:  # Use the lock when accessing awaited_response_id
            if wait_for_response:
                self.awaited_response_id = request_id
            else:
                self.awaited_response_id = 0

        request = {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": method,
            "params": params,
        }
        message = json.dumps(request)
        content_length = len(message)
        communication_string = (
            f"Content-Length: {content_length}\r\n\r\n{message}"
        )
        self.process.stdin.write(communication_string.encode())
        self.process.stdin.flush()

    @staticmethod
    def content_length(line: bytes) -> int | None:
        if line.startswith(b"Content-Length: "):
            _, value = line.split(b"Content-Length: ")
            value = value.strip()
            try:
                return int(value)
            except ValueError as err:
                raise ValueError(
                    f"Invalid Content-Length header: {value.decode()}"
                ) from err
        return None

    def read_response(self):
        while True:
            sys.stdout.flush()
            stdout = self.process.stdout.readline()
            # TODO: Handle stderr
            if not stdout:
                break
            try:
                num_bytes = self.content_length(stdout)
            except ValueError:
                continue
            if num_bytes is None:
                continue
            while stdout and stdout.strip():
                stdout = self.process.stdout.readline()
            if not stdout:
                break
            body = self.process.stdout.read(num_bytes)
            response = json.loads(body)
            with self.lock:
                self.responses.append(response)
            received_response_id = response.get("id", 0)
            with self.lock:
                if received_response_id == self.awaited_response_id:
                    logger.debug(
                        "Received awaited response with id %d",
                        received_response_id,
                    )
                    sys.stdout.flush()
                    self.request_processed_event.set()

    def response_by_id(self, request_id) -> dict[str, t.Any] | None:
        with self.lock:
            for response in self.responses:
                if response.get("id") == request_id:
                    return response
        return None


@main.command()
@click.option(
    "--java-execution-environment",
    type=click.Choice(
        [
            "JavaSE-17",
            "JavaSE-18",
            "JavaSE-19",
            "JavaSE-20",
            "JavaSE-21",
            "JavaSE-22",
        ]
    ),
    required=True,
    help=(
        "The Java execution environment to be used. The value must be an"
        " exact match of the execution environment name as it appears in"
        " the enumeration named `ExecutionEnvironment` as defined here:"
        " https://github.com/eclipse-jdtls/eclipse.jdt.ls/wiki/"
        "Running-the-JAVA-LS-server-from-the-command-line#initialize-request"
    ),
)
@click.argument("java_home", type=click.Path(exists=True, dir_okay=True))
@click.argument("jdtls_home", type=click.Path(exists=True, dir_okay=True))
def build_workspace(
    java_execution_environment: str,
    java_home: pathlib.Path,
    jdtls_home: pathlib.Path,
) -> None:
    """Build (headless) an Eclipse Java project's workspace.

    \b
    Arguments
    ---------
    java_home : pathlib.Path
        The path to the Java (JDK) home directory.
    jdtls_home : pathlib.Path
        The path to the Eclipse JDT Language Server (JDTLS) installation
        directory.
    """  # noqa: D301
    java_executable = pathlib.Path(java_home).resolve() / "bin" / "java"
    jdtls_home = pathlib.Path(jdtls_home).resolve()
    if not java_executable.is_file():
        click.echo(f"Java executable `{java_executable}` not found.")
        sys.exit(1)
    if not jdtls_home.is_dir():
        click.echo(f"JDTLS home directory `{jdtls_home}` not found.")
        sys.exit(1)
    cwd = pathlib.Path.cwd()
    click.echo(f"Building workspace for project at `{cwd}`...")
    pathlib.Path("jdtls_stdout.log").unlink(missing_ok=True)
    pathlib.Path("jdtls_stderr.log").unlink(missing_ok=True)
    for filename in (
        ".classpath",
        ".project",
        "build.properties",
        "plugin.xml",
        "pom.xml",
    ):
        file_path = cwd / filename
        if not file_path.is_file():
            click.echo(f"File `{file_path}` not found.")
            sys.exit(1)
    try:
        jdtls_jar = next(
            pathlib.Path(f"{jdtls_home}/plugins").glob(
                "org.eclipse.equinox.launcher_*.jar"
            )
        )
    except StopIteration:
        click.echo(
            f"Cannot find JDTLS jar in `{jdtls_home}/plugins`"
            f" Tried to find `org.eclipse.equinox.launcher_*.jar`."
        )
        sys.exit(1)
    # Start the language server as a subprocess
    process: subprocess.Popen = subprocess.Popen(
        [
            str(java_executable),
            "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Declipse.product=org.eclipse.jdt.ls.core.product",
            "-Dlog.protocol=true",
            "-Dlog.level=DEBUG",
            "-Xmx1g",
            "--add-modules=ALL-SYSTEM",
            "--add-opens",
            "java.base/java.util=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.lang=ALL-UNNAMED",
            "-jar",
            str(jdtls_jar),
            "-configuration",
            f"{jdtls_home}/config_linux",
            "-data",
            "/tmp/ws",
        ],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    lsp_client = LSPClient(process)
    response_thread = threading.Thread(
        target=lsp_client.read_response,
        daemon=True,
    )
    response_thread.start()

    # key: method, val: params
    requests: list[dict[str, int | str | dict | bool]] = [
        {
            # https://github.com/Microsoft/language-server-protocol/blob/main/versions/protocol-2-x.md#initialize
            "id": next(LSPClient.id_generator),
            "method": "initialize",
            "params": {
                "processId": os.getpid(),
                "rootPath": str(cwd),
                "rootUri": f"file://{cwd}",
                "trace": "off",
                "workDoneToken": "1",
                "workspaceFolders": [
                    {
                        "name": str(cwd),
                        "uri": f"file://{cwd}",
                    }
                ],
            },
            "wait_for_response": True,
        },
        {
            "id": next(LSPClient.id_generator),
            "method": "initialized",
            "params": {},
            "wait_for_response": False,
        },
        {
            "id": next(LSPClient.id_generator),
            "method": "workspace/didChangeConfiguration",
            "params": {
                "settings": {
                    "java": {
                        "format": {"enabled": False},
                        "home": str(java_home),
                        "configuration": {
                            "runtimes": [
                                {
                                    "name": java_execution_environment,
                                    "path": f"{java_home}/",
                                }
                            ]
                        },
                        "trace": {"server": "verbose"},
                    }
                }
            },
            "wait_for_response": False,
        },
        {
            "id": next(LSPClient.id_generator),
            "method": "java/buildWorkspace",
            "params": True,
            "wait_for_response": True,
        },
        {
            # https://github.com/Microsoft/language-server-protocol/blob/main/versions/protocol-2-x.md#shutdown-request
            "id": next(LSPClient.id_generator),
            "method": "shutdown",
            "params": {},
            "wait_for_response": True,
        },
        {
            # https://github.com/Microsoft/language-server-protocol/blob/main/versions/protocol-2-x.md#exit-notification
            "id": next(LSPClient.id_generator),
            "method": "exit",
            "params": {},
            "wait_for_response": False,
        },
    ]
    for request in requests:
        lsp_client.send_request(request)
        if request["wait_for_response"]:
            lsp_client.request_processed_event.wait()
            response = lsp_client.response_by_id(request["id"])
            logger.debug(
                "Received response: %s", json.dumps(response, indent=2)
            )
        else:
            continue
        if request["method"] == "java/buildWorkspace":
            if response is None:
                click.echo(
                    "Build of workspace failed with"
                    " an unknown error. No response received."
                )
                sys.exit(1)
            status = response.get("result", BuildWorkspaceStatus.FAILED.value)
            if status == BuildWorkspaceStatus.SUCCEED.value:
                click.echo("Build of workspace succeeded.")
                sys.exit(0)
            elif status == BuildWorkspaceStatus.CANCELLED.value:
                click.echo("Build of workspace cancelled.")
            elif status == BuildWorkspaceStatus.WITH_ERROR.value:
                click.echo("Build of workspace failed with error.")
            else:
                click.echo("Build of workspace failed with an unknown error.")
            sys.exit(status)
    process.wait()
    response_thread.join()


@main.command()
@click.argument("target_path", type=click.Path(exists=True, dir_okay=True))
def deploy(target_path: pathlib.Path) -> None:
    """Deploy the eclipse plugin.

    \b
    Arguments
    ---------
    target_path : pathlib.Path
        The installation directory of an Eclipse/ Capella application
        where the plugin will be deployed into the subdirectory `dropins`.
    """  # noqa: D301
    target_path = pathlib.Path(target_path) / "dropins"
    if not target_path.is_dir():
        click.echo(f"Target directory `{target_path}` not found.")
        sys.exit(1)
    _, jar_path = _output_and_jar_path()
    dest = target_path / jar_path.name
    dest.unlink(missing_ok=True)
    shutil.copy(jar_path, dest)
    if dest.is_file():
        click.echo(f"Deployed `{dest.resolve()}`.")


def _get_bundle_classpath(third_party_lib_paths: list[pathlib.Path]) -> str:
    lib_paths = sorted([p.name for p in third_party_lib_paths])
    value = "."
    if third_party_lib_paths:
        value = ".,\n"
        value += ",\n".join(f" lib/{p}" for p in lib_paths)
    return f"Bundle-ClassPath: {value}"


def _update_bundle_classpath(
    third_party_lib_paths: list[pathlib.Path],
) -> None:
    manifest = MANIFEST_PATH.read_text(encoding="utf-8")
    bundle_classpath = _get_bundle_classpath(third_party_lib_paths)
    lines = manifest.splitlines()
    manifest = ""
    found_bundle_classpath = False
    inside_bundle_classpath = False
    for line in lines:
        if line.startswith("Bundle-ClassPath:"):
            found_bundle_classpath = True
            manifest += bundle_classpath + "\n"
            inside_bundle_classpath = True
            continue
        if inside_bundle_classpath:
            if line.startswith(" "):
                continue
            inside_bundle_classpath = False
        manifest += line.rstrip() + "\n"
    if bundle_classpath and not found_bundle_classpath:
        if not manifest.endswith("\n"):
            manifest += "\n"
        manifest += bundle_classpath + "\n"
    # TODO: ensure that the maximum line length (72) is not exceeded
    MANIFEST_PATH.write_text(manifest, encoding="utf-8")


@main.command()
@click.argument("java_home", type=click.Path(exists=True, dir_okay=True))
@click.argument(
    "target_platform_path", type=click.Path(exists=True, dir_okay=True)
)
def package(
    java_home: pathlib.Path, target_platform_path: pathlib.Path
) -> None:
    """Package the eclipse plugin.

    \b
    Arguments
    ---------
    java_home : pathlib.Path
        The path to the Java home directory.
    target_platform_path
        The installation directory of an Eclipse/ Capella application
        that will be referenced as target platform to build the
        classpath.
    """  # noqa: D301
    lib_dir = pathlib.Path("lib")
    if lib_dir.is_dir():
        shutil.rmtree(lib_dir)
    lib_dir.mkdir()
    third_party_lib_paths = [
        p
        for p in _third_party_lib_paths()
        if not p.is_relative_to(target_platform_path)
    ]
    if third_party_lib_paths:
        for path in third_party_lib_paths:
            dest = lib_dir / path.name
            dest.unlink(missing_ok=True)
            shutil.copy(path, dest)
    _update_bundle_classpath(third_party_lib_paths)
    for path in (MANIFEST_PATH, PLUGIN_XML_PATH):
        if not path.is_file():
            click.echo(f"`{path}` file not found.")
            sys.exit(1)
    output_path, jar_path = _output_and_jar_path()
    logger.info("Packaging the addon into `%s`...", jar_path)
    jar_path.unlink(missing_ok=True)
    jar = pathlib.Path(java_home) / "bin" / "jar"
    jar_cmd = [
        str(jar),
        "cfm",
        str(jar_path),
        str(MANIFEST_PATH),
        "-C",
        f"{output_path}/",
        ".",
        str(PLUGIN_XML_PATH),
    ]
    potential_additional_dirs = (
        "lib",
        "OSGI-INF",
    )
    for dir_ in potential_additional_dirs:
        if pathlib.Path(dir_).is_dir() and list(pathlib.Path(dir_).iterdir()):
            jar_cmd.append(f"{dir_}/")
    jar_path.parent.mkdir(parents=True, exist_ok=True)
    click.echo(f"Running command: {' '.join(jar_cmd)}")
    subprocess.run(jar_cmd, check=True)
    if jar_path.is_file():
        click.echo(f"Created `{jar_path.resolve()}`.")


# Define another subcommand
@main.command()
def clean() -> None:
    """Clean the build artifacts."""
    click.echo("Cleaning build artifacts...")


if __name__ == "__main__":
    main()
