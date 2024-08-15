#!/usr/bin/env zsh
SCRIPT_PATH=$(realpath -s $0)
SCRIPT_DIR=$(dirname $SCRIPT_PATH)
openapi-generator-cli generate \
    -i openapi/custom.yaml \
    -c $SCRIPT_DIR/jaxrs-jersey.json \
    -g jaxrs-jersey \
    -o .
