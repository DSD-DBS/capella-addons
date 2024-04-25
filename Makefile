PATH_TO_CAPELLA ?= /path/on/host/to/capella
include .env

build:
	docker build -t capella-plugins-dev .devcontainer

run:
	docker run --rm -it --hostname=devcontainer \
		-v /tmp/.X11-unix:/tmp/.X11-unix \
		-v /tmp/dev:/root/dev \
		-v $(PATH_TO_CAPELLA):/opt/capella \
		capella-plugins-dev