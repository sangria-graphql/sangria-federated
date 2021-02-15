**sangria-federated** is a library that allows sangria users to implement services that adhere to 
[Apollo's Federation Specification](https://www.apollographql.com/docs/federation/federation-spec/),
and can be used as part of a federated data graph.

[![License](http://img.shields.io/:license-Apache%202-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Join the chat at https://gitter.im/sangria-graphql/sangria](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sangria-graphql/sangria?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## How does it work?

The library adds [Apollo's Federation Specification](https://www.apollographql.com/docs/federation/federation-spec/)
on top of the provided sangria graphql schema. And to make it possible to use *_Any* as a scalar,
the library upgrades the marshaller used in the user application too.

## Caution 🚨🚨

The library upgrades the marshaller too, by making map values scalars (e.g. json objects as 
scalars). This if not treated with caution, can lead to security issues as discussed [here](http://www.petecorey.com/blog/2017/06/12/graphql-nosql-injection-through-json-types/).

## Contribute

Contributions are warmly desired; please follow the standard process of forking the repo, creating a
branch then making a PR.

## License

**sangria-federated** is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

