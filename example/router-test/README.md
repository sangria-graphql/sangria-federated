# Apollo Router project

```
# first start the examples (sbt start-all)

# create the supergraph
~/.rover/bin/rover supergraph compose --elv2-license accept --config ../router/supergraph-local.yaml > target/supergraph-local.graphql

cargo run -- --config router.yaml --supergraph target/supergraph-local.graphql

APOLLO_ROUTER_CONFIG_PATH=router.yaml \
APOLLO_ROUTER_SUPERGRAPH_PATH=target/supergraph-local.graphql \
cargo watch -c -x "run"
```
