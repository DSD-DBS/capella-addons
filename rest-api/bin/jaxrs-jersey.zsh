#!/usr/bin/env zsh
# Copyright DB InfraGO AG and contributors
# SPDX-License-Identifier: CC0-1.0
SCRIPT_PATH=$(realpath -s $0)
SCRIPT_DIR=$(dirname $SCRIPT_PATH)
openapi-generator generate \
    -i openapi/custom.yaml \
    -c $SCRIPT_DIR/jaxrs-jersey.json \
    -g jaxrs-jersey \
    -o .
