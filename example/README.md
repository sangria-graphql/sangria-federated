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

The public GraphQL schema is exposed, by the federation gateway, under: http://localhost:9080/

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
