# Contributing

## Development container with setup to develop Capella plugins

This repository comes with a subdirectory `.devcontainer` that contains a
`Dockerfile`. This `Dockerfile` is based on a Fedora image and installs all
dependencies needed to develop Capella plugins.

For now the `Dockerfile` comes with everything needed to develop Capella
plugins using Neovim.


Build the container via the following command:

```bash
docker build -t capella-plugins-dev .devcontainer
```

Run the container via the following command where a Linux Capella with an
appropiate architecture (x86_64 or aarch64 depending on the Docker host)
is mounted to `/opt/capella`:

```bash
docker run --rm -it --hostname=devcontainer -v /tmp/dev:/root/dev -v /path/on/host/to/capella_x.y.z.app:/opt/capella capella-plugins-dev
# for example:
docker run --rm -it --hostname=devcontainer -v /tmp/dev:/root/dev -v /Users/jamilraichouni/Applications/Capella_6.0.0.app:/opt/capella capella-plugins-dev
```

to build plugins for Capella v.x.y

## Workflow

1. Decide for which Capella version you want to develop the plugin for and
   adapt the `docker run` command from above accordingly.
1. Start the container
