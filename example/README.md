### Start/stop all the services

```
sbt start-all
sbt stop-all
```

### Start the federation router

```
cd example # if not already
cd router
./start.sh
```

### Using the exposed GraphQL endpoint

The public GraphQL schema is exposed, by the federation gateway, under: http://localhost:4000/

Example of queries:
```
{
  reviews(ids: [34, 54]) {
    id
    key
    state {
      id
      key
    }
  }
}
```
