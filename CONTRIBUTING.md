<!--
  Copyright DB InfraGO AG and contributors
  SPDX-License-Identifier: Apache-2.0
-->

# Contributing

## Development container with setup to develop Capella plugins

### Build container

This repository comes with a subdirectory `.devcontainer` that contains a
`Dockerfile`. This `Dockerfile` is based on a Fedora image and installs all
dependencies needed to develop Capella plugins.

For now the `Dockerfile` comes with everything needed to develop Capella
plugins using Neovim.

Build the container via the following command:

```bash
docker build -t capella-plugins-dev .devcontainer
```

### Run container on macOS

Precondition:

- Download and install XQuartz from https://www.xquartz.org/
- Optionally: Register `XQuartz` as login item (macOS' autostart)
- Open the `XQuartz.app` and open the settings (menu). Got to tab "Security"
  and tick the checkbox "Allow connections from network clients"
- Restart `XQuartz`
- Run a container via

  ```bash
  docker run (...) \
      -v /tmp/.X11-unix:/tmp/.X11-unix (...)
  ```

### Run container on Windows using WSL and WSLg

Precondition: Install WSL and a distribution of your choice.

Due to a bug in WSLg, the X11 socket sometimes doesn't get mounted properly. We
can work around this by directly using the source path:

```bash
docker run (...) -v /mnt/wslg/.X11-unix:/tmp/.X11-unix (...)
```

### Work on a Capella plugin project

Store some Capella development project locally at `/tmp/projects`.
You may want to command

```bash
mkdir /tmp/projects
git clone --branch feat-models-from-directory-importer \
  git@github.com:DSD-DBS/capella-addons.git \
  /tmp/projects/capella-addons
```

Run the container via the following command where a Capella is mounted to
`/opt/capella`.

We you just want to build and not debug a plugin you can mount a local macOS
Capella installation to `/opt/capella`.

To be able to debug a plugin during development run the container via the
following command where a Linux Capella with an appropiate architecture (x86_64
or aarch64 depending on the Docker host) is mounted to `/opt/capella`:

```bash
docker run --rm -it --hostname=devcontainer \
  -v /tmp/.X11-unix:/tmp/.X11-unix \
  -v /tmp/projects:/root/projects \
  -v /path/on/host/to/a/capella[\.app]?:/opt/capella \
  capella-plugins-dev
# for example:
docker run --rm -it --hostname=devcontainer \
  -v /tmp/.X11-unix:/tmp/.X11-unix \
  -v /tmp/projects:/root/projects \
  -v /Users/jamilraichouni/Applications/Capella_6.0.0.app:/opt/capella \
  capella-plugins-dev
```

to build plugins for Capella v.x.y

## Workflow

1. Decide for which Capella version you want to develop the plugin for and
   adapt the `docker run` command from above accordingly.
1. Start the container
