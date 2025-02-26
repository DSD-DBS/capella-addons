<!--
 ~ Copyright DB InfraGO AG and contributors
 ~ SPDX-License-Identifier: Apache-2.0
 -->

# Contributing

## Common

Clone the repository and change into the root directory of the repository.

Capella addons are developed on separate branches. To contribute, please
follow these steps:

1. When you want to add a new addon, create a new branch from the `main`
   branch and name it according to the Capella addon (name as defined in the
   next step) you plan to contribute. Create the a new addon project using the
   [project template] which has according instructions or work on a given addon
   project.
1. When you want to work on an existing addon, just checkout the branch of the
   addon you want to work on.

## Preconditions

Python and a JDK version that fits the desired Capella target platform must be
installed.

An Eclipse JDT Language Server (JDTLS) must be installed.
You can just download https://download.eclipse.org/jdtls/milestones/1.40.0/jdt-language-server-1.40.0-202409261450.tar.gz
and extract it to a directory of your choice.

A Capella installation must be available.

The CLI of the present project must be installed, since it is used to manage
development tasks. Change into the repository root directory and run:

```shellscript
pip install .
```

This makes a CLI program named `capella-addons` available in your shell.
Run the command `capella-addons --help` for more information.

## Development workflow

1. Change into the directory of the Eclipse/ Capella addon project (sub folder
   of the repository).

1. Run the `capella-addons build-classpath` command to generate the
   `.classpath` file, example:

   ```shell
   capella-addons build-classpath \
     --java-execution-environment=JavaSE-17 \
     src/com/db/capella/api/impl/ProjectsApiServiceImpl.java \
     /opt/capella_6.0.0
    ```

1. Run the `capella-addons build-workspace` command to build the workspace of
   the Eclipse/ Capella addon project, example:

   ```shell
   capella-addons build-workspace \
     --java-execution-environment=JavaSE-17 \
     /usr/lib/jvm/jdk-17.0.6+10 \
     /path/to/jdtls
   ```

1. Run the `capella-addons package` command to package the Eclipse/ Capella
   the Eclipse/ Capella addon project, example:

   ```shell
   capella-addons package /usr/lib/jvm/jdk /opt/capella_6.0.0
   ```
   This creates a `.jar` file (Capella dropin) in the
   `capella-addons/ADDON_NAME/target/` directory of the project.

## Release workflow

1. Open a pull request to merge the changes on the `ADDON_NAME` branch into the
   `main` branch.

1. After the pull request has been merged, create a new release on GitHub.
   To create a release just add a new tag `ADDON_NAME/vX.Y.Z`.

[project template]: https://github.com/DSD-DBS/cookiecutter-dbs-eclipse-addon
