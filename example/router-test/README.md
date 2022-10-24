# Apollo Router project

```
cargo run -- --config router.yaml --supergraph ../router/target/supergraph-local.graphql

APOLLO_ROUTER_CONFIG_PATH=router.yaml \
APOLLO_ROUTER_SUPERGRAPH_PATH=../router/target/supergraph-local.graphql \
cargo watch -c -x "run"
```
