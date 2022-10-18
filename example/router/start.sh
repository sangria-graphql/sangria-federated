#!/usr/bin/env bash

set -euo pipefail

if [ ! -d target ]; then
  mkdir target
fi

pushd target
if [ ! -f ~/.rover/bin/rover ]; then
  curl -sSL https://rover.apollo.dev/nix/latest | sh
fi
~/.rover/bin/rover supergraph compose --elv2-license accept --config ../supergraph-local.yaml > supergraph-local.graphql

if [ ! -f ./router ]; then
  curl -sSL https://router.apollo.dev/download/nix/latest | sh
fi
./router -c ../router.yaml -s supergraph-local.graphql
popd
