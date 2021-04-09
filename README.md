![Sangria Logo](https://sangria-graphql.github.io/assets/img/sangria-logo.svg)

# sangria-federated

![Continuous Integration](https://github.com/sangria-graphql/sangria-federated/workflows/Continuous%20Integration/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.sangria-graphql/sangria-federated_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.sangria-graphql/sangria-federated_2.13)
[![License](http://img.shields.io/:license-Apache%202-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Scaladocs](https://www.javadoc.io/badge/org.sangria-graphql/sangria-federated_2.13.svg?label=docs)](https://www.javadoc.io/doc/org.sangria-graphql/sangria-federated_2.13)
[![Join the chat at https://gitter.im/sangria-graphql/sangria](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sangria-graphql/sangria?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

**sangria-federated** is a library that allows sangria users to implement services that adhere to
[Apollo's Federation Specification](https://www.apollographql.com/docs/federation/federation-spec/), and can be used as
part of a federated data graph.

SBT Configuration:

```scala
libraryDependencies += "org.sangria-graphql" %% "sangria-federated" % "latest.release"
```

## How does it work?

The library adds [Apollo's Federation Specification](https://www.apollographql.com/docs/federation/federation-spec/)
on top of the provided sangria graphql schema.

To make it possible to use `_Any` as a scalar, the library upgrades the used marshaller.

## How to use it?

To be able to communicate with [Apollo's federation gateway](https://www.apollographql.com/docs/federation/gateway/), 
the graphql sangria service should be using both the federated schema and unmarshaller.

As an example, let's consider an application using circe with a state and review service. 
- The state service defines the state entity, annotated with ```@key("id")```. And for each entity, we need to define an
  entity resolver ([reference resolver](https://www.apollographql.com/docs/federation/entities/#resolving)), see code 
  below:
    ```scala
    import sangria.federation.Decoder
    import io.circe.Json,  io.circe.generic.semiauto._
    import sangria.schema._
    
    case class State(
      id: Int,
      key: String,
      value: String)
    
    object State {
    
      case class StateArg(id: Int)
    
      implicit val decoder: Decoder[Json, StateArg] = deriveDecoder[StateArg].decodeJson(_)
    
      val stateResolver = EntityResolver[StateService, Json, State, StateArg](
        __typeName = "State",
        ev = State.decoder,
        arg =>  env.getState(arg.id))
    
      implicit val stateSchema =
        ObjectType(
          "State",
          fields[Unit, State](
            Field(
              "id", IntType,
              resolve = _.value.id),
            Field(
              "key", StringType,
              resolve = _.value.key),
            Field(
              "value", StringType,
              resolve = _.value.value))
        ).copy(astDirectives = Vector(federation.Directives.Key("id")))
    }
    ```
  
    The entity resolver implements:
    - the deserialization of the fields in `_Any` object to the EntityArg.
    - how to fetch the EntityArg to get the proper Entity (in our case State).
    
    As for the query type, let's suppose the schema below:
    ```scala
    import sangria.schema._
    
    object StateAPI {
    
    val Query = ObjectType(
      "Query",
      fields[StateService, Unit](
        Field(
          name = "states", 
          fieldType = ListType(State.stateSchema), 
          resolve = _.ctx.getStates)))
    }
    ```
  
    Now in the definition of the GraphQL server, we federate the Query type and the unmarshaller while supplying the
    entity resolvers. Then, we use both the federated schema and unmarshaller as arguments for the server.
    ```scala
    def graphQL[F[_]: Effect]: GraphQL[F] = {
      val (schema, um) = federation.Federation.federate[StateService, Json](
        Schema(StateAPI.Query),
        sangria.marshalling.circe.CirceInputUnmarshaller,
        stateResolver)
    
      GraphQL(
        schema,
        env.pure[F])(implicitly[Effect[F]], um)
    }
    ```
  
    And, the GraphQL server should use the provided schema and unmarshaller as arguments for the sangria executor:
    ```scala
    import cats.effect._
    import cats.implicits._
    import io.circe._
    import sangria.ast.Document
    import sangria.execution._
    import sangria.marshalling.InputUnmarshaller
    import sangria.marshalling.circe.CirceResultMarshaller
    import sangria.schema.Schema
  
    object GraphQL {

      def apply[F[_], A](
        schema: Schema[A, Unit],
        userContext: F[A]
      )(implicit F: Async[F], um: InputUnmarshaller[Json]): GraphQL[F] = new GraphQL[F] {

        import scala.concurrent.ExecutionContext.Implicits.global
        
        def exec(
          schema:        Schema[A, Unit],
          userContext:   F[A],
          query:         Document,
          operationName: Option[String],
          variables:     Json): F[Either[Json, Json]] = userContext.flatMap { ctx =>
          
            F.async { (cb: Either[Throwable, Json] => Unit) =>
              Executor.execute(
                schema           = schema,
                queryAst         = query,
                userContext      = ctx,
                variables        = variables,
                operationName    = operationName,
                exceptionHandler = ExceptionHandler {
                  case (_, e) ⇒ HandledException(e.getMessage)
                }
              ).onComplete {
                case Success(value) => cb(Right(value))
                case Failure(error) => cb(Left(error))
              }
            }
          }.attempt.flatMap {
            case Right(json)               => F.pure(json.asRight)
            case Left(err: WithViolations) => ???
            case Left(err)                 => ???
          }
      }
    }
    ```
  
- The review service defines the review type, which has a reference to the state type. And, for each entity referenced
  by another service, a [stub type](https://www.apollographql.com/docs/federation/entities/#referencing) should be 
  created (containing just the minimal information that will allow to reference the entity).
  ```scala
  import sangria.schema._
  
  case class Review(
    id: Int,
    key: Option[String] = None,
    state: State)
    
  object Review {

    implicit val reviewSchema =
      ObjectType(
        "Review",
        fields[Unit, Review](
          Field(
            "id", IntType,
            resolve = _.value.id),
          Field(
            "key", OptionType(StringType),
            resolve = _.value.key),
          Field(
            "state", State.stateSchema,
            resolve = _.value.state)))
  }
  
  case class State(id: Int)

  object State {
    
    import sangria.federation.Directives._
  
    implicit val stateSchema =
      ObjectType(
        "State",
        fields[Unit, State](
          Field[Unit, State, Int, Int](
            name = "id",
            fieldType = IntType,
            resolve = _.value.id).copy(astDirectives = Vector(External)))
      ).copy(astDirectives = Vector(Key("id"), Extends))
  }
  ```
  
  In the end, the same code used to federate the state service is used to federate the review service.


- The sangria GraphQL services endpoints can now be configured in the ```serviceList``` of [Apollo's Gatewqay](
  https://www.apollographql.com/docs/federation/gateway/#setup) as follows:
    ```
    const gateway = new ApolloGateway({
      serviceList: [
        { name: 'states', url: 'http://localhost:9081/api/graphql'},
        { name: 'reviews', url: 'http://localhost:9082/api/graphql'}
      ],
      debug: true
    })
    ```
  
All the code of the example is available [here](./example).

## Caution 🚨🚨

- **This is a technology preview and should not be used in a production environment.**
- The library upgrades the marshaller too, by making map values scalars (e.g. json objects as scalars). This can lead to
  security issues as discussed [here](
  http://www.petecorey.com/blog/2017/06/12/graphql-nosql-injection-through-json-types/).

## Contribute

Contributions are warmly desired 🤗. Please follow the standard process of forking the repo and making PRs 🤓

## License

**sangria-federated** is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

