### Start/stop all the services

```
sbt start-all
sbt stop-all
```

### Start gateway

```
cd gateway
npm install && npm start
```

### Using the exposed GraphQL endpoint

The public GraphQL schema is exposed by the federation gateway running under: http://localhost:9080/

Example of queries:
```
{
  reviews {
    id
    key
    state {
      id
      key
    }
  }
}
```

### Federated GraphQL endpoints

Federated services are running under:
- http://localhost:9081
- http://localhost:9082

To explicitly check that json can be accepted as scalars, visit http://localhost:9081 and execute these queries:

- for InputObject notation
    ```
    {
      _entities(representations: [{__typename: "State", id: 0}]) {
        __typename
        ... on State {
          id
          key
        }
      }
    }
    ```

- for json
    ```
    query($representations: [_Any!]!) {
      _entities(representations: $representations) {
        __typename
        ... on State {
          id
          key
        }
      }
    }
    ```
  and in the variables panel
    ```
    {
      "representations": [{"__typename": "State", "id": 0}]
    }
    ```
