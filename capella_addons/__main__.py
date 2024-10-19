# Copyright DB InfraGO AG and contributors
# SPDX-License-Identifier: Apache-2.0
import contextlib
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile
import threading

import click
import lxml.builder
import lxml.etree

import capella_addons

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
@click.version_option(
    version=capella_addons.__version__,
    prog_name="eclipse-plugin-builders",
    message="%(prog)s %(version)s",
)
def main() -> None:
    """Console tools to develop, build, pack, and deploy Capella addons.

    The present CLI needs the command line tools `jar` and `mvn` to be
    installed and accessible via the user's PATH.

    `jar` is part of the Java Development Kit (JDK) and is used to
    create the jar file for a Capella addon.

    `mvn` is the Maven build tool and is used to analyse the
    dependencies listed in the `pom.xml` file to build the `.classpath`
    file for a Capella addon project.
    """


def _read_response_from_eclipse_jdt_ls(process):
    while True:
        stdout = process.stdout.readline()
        stderr = process.stderr.readline()
        if not stdout and not stderr:
            continue
        print(stdout.decode("utf-8").strip(), file=sys.stdout)
        print(stderr.decode("utf-8").strip(), file=sys.stderr)


def _send_remote_procedure_request_to_eclipse_jdt_ls(process, method, params):
    """Send a remote procedure request to the Eclipse JDT language server."""
    request = {"jsonrpc": "2.0", "id": 1, "method": method, "params": params}
    message = json.dumps(request)
    content_length = len(message)
    communication_string = f"Content-Length: {content_length}\r\n\r\n{message}"
    process.stdin.write(communication_string.encode("utf-8"))
    process.stdin.flush()


def _third_party_lib_paths() -> list[pathlib.Path]:
    """Return the paths to the third-party libraries."""
    classpath_root = _read_xml_file(".classpath")
    third_party_lib_paths = classpath_root.xpath(
        'classpathentry[@kind="lib" and '
        'not(starts-with(@path, "/opt/capella_6.0.0"))]/@path'
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
    return f"{group_id}.{artifact_id}_{version}.jar"


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
    target_classpaths.sort(key=lambda x: x.get("path"))
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
        print(f"Building classpath for project in `{project_dir}`")
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


@main.command()
def build_workspace() -> None:
    """Build (headless) an Eclipse Java project's workspace."""
    click.echo("Building workspace...")
    # Start the language server as a subprocess
    process = subprocess.Popen(
        [
            "java",
            "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Declipse.product=org.eclipse.jdt.ls.core.product",
            "-Dlog.protocol=true",
            "-Dlog.level=ALL",
            "-Xmx1g",
            "--add-modules=ALL-SYSTEM",
            "--add-opens",
            "java.base/java.util=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.lang=ALL-UNNAMED",
            "-jar",
            "/home/nörd/.local/share/nvim/mason/share/jdtls/plugins/org.eclipse.equinox.launcher.jar",
            "-configuration",
            "/home/nörd/.local/share/nvim/mason/packages/jdtls/config_linux_arm",
            "-data",
            "/tmp/ws",
        ],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )

    # Start a thread to read responses from the language server
    threading.Thread(
        target=_read_response_from_eclipse_jdt_ls, args=(process,), daemon=True
    ).start()


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
    lib_paths = sorted([p.name for p in _third_party_lib_paths()])
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
def package() -> None:
    """Package the eclipse plugin."""
    lib_dir = pathlib.Path("lib")
    if lib_dir.is_dir():
        shutil.rmtree(lib_dir)
    lib_dir.mkdir()
    third_party_lib_paths = _third_party_lib_paths()
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
    jar_path.unlink(missing_ok=True)
    jar_cmd = [
        "jar",
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
