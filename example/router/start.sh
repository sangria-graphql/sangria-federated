#!/usr/bin/env bash

set -euo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ ! -d "$DIR"/target ]; then
  mkdir "$DIR"/target
fi

pushd "$DIR"/target
if [ ! -f ~/.rover/bin/rover ]; then
  curl -sSL https://rover.apollo.dev/nix/latest | sh
fi
APOLLO_TELEMETRY_DISABLED=1 ~/.rover/bin/rover supergraph compose --elv2-license accept --config ../supergraph-local.yaml > supergraph-local.graphql

if [ ! -f ./router ]; then
  curl -sSL https://router.apollo.dev/download/nix/latest | sh
fi
APOLLO_TELEMETRY_DISABLED=1 ./router -c ../router.yaml -s supergraph-local.graphql
popd
