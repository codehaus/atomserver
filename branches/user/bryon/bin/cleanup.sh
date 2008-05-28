#!/bin/sh

export BIN_DIR=$(cd `dirname $0`; pwd)

$BIN_DIR/clear-db.sh

$BIN_DIR/clean-test-files.sh

