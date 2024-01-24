# federated subgraph to test apollo federation spec compatibility

Implementation of a federated subgraph aligned to the requirements outlined in [apollo-federation-subgraph-compatibility](https://github.com/apollographql/apollo-federation-subgraph-compatibility).

The subgraph can be used to verify compatibility against [Apollo Federation Subgraph Specification](https://www.apollographql.com/docs/federation/subgraph-spec/).

### Run compatibility test
Execute the following command from the repo root
```
npx @apollo/federation-subgraph-compatibility docker --compose example/product/docker-compose.yaml --schema example/product/products.graphql
```

### Printing the GraphQL schema (SQL)

```
sbt "example-product/run printSchema"
```
