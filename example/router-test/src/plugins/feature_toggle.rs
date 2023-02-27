use apollo_compiler::hir::TypeSystem;
use apollo_compiler::{ApolloCompiler, ApolloDiagnostic, HirDatabase};
use apollo_parser::ast::{AstNode, Definition};
use apollo_parser::Parser;

use apollo_router::_private::router_bridge::introspect;
use apollo_router::_private::router_bridge::planner::QueryPlannerConfig;
use apollo_router::layers::ServiceBuilderExt;
use apollo_router::plugin::Plugin;
use apollo_router::plugin::PluginInit;
use apollo_router::register_plugin;
use apollo_router::services::supergraph;
use schemars::JsonSchema;
use serde::Deserialize;
use std::ops::ControlFlow;
use std::sync::Arc;
use tower::BoxError;
use tower::ServiceBuilder;
use tower::ServiceExt;

#[derive(Debug)]
struct RawSupergraph {
    sdl: Arc<String>,
}

impl RawSupergraph {
    fn new(sdl: Arc<String>) -> RawSupergraph {
        RawSupergraph { sdl }
    }

    fn for_features(&self, features: Vec<String>) -> anyhow::Result<PublicGraph> {
        let parser = Parser::new(&self.sdl);
        let ast = parser.parse();
        assert_eq!(ast.errors().len(), 0);

        let doc = ast.document();

        let mut pub_schema = apollo_encoder::Document::new();
        for definition in doc.definitions() {
            match definition {
                Definition::SchemaDefinition(e) => pub_schema.schema(e.try_into()?),
                Definition::OperationDefinition(e) => pub_schema.operation(e.try_into()?),
                Definition::FragmentDefinition(e) => pub_schema.fragment(e.try_into()?),
                Definition::DirectiveDefinition(e) => pub_schema.directive(e.try_into()?),
                Definition::ScalarTypeDefinition(e) => pub_schema.scalar(e.try_into()?),
                Definition::ObjectTypeDefinition(e) => {
                    let mut o =
                        apollo_encoder::ObjectDefinition::new(e.name().unwrap().source_string());
                    if let Some(directives) = e.directives() {
                        for d in directives.directives() {
                            o.directive(d.try_into()?);
                        }
                    }
                    if let Some(fields) = e.fields_definition() {
                        for f in fields.field_definitions() {
                            if features.is_empty() {
                                let mut to_add = true;
                                if let Some(directives) = f.directives() {
                                    for d in directives.directives() {
                                        if let Some(name) = d.name() {
                                            if name.source_string() == "feature" {
                                                to_add = false;
                                            }
                                        }
                                    }
                                }
                                if to_add {
                                    o.field(f.try_into()?);
                                }
                            } else {
                                // for the prototype, always add the field even if the feature name is not the good one
                                o.field(f.try_into()?);
                            }
                        }
                    }
                    pub_schema.object(o);
                }
                Definition::InterfaceTypeDefinition(e) => pub_schema.interface(e.try_into()?),
                Definition::UnionTypeDefinition(e) => pub_schema.union(e.try_into()?),
                Definition::EnumTypeDefinition(e) => pub_schema.enum_(e.try_into()?),
                Definition::InputObjectTypeDefinition(e) => pub_schema.input_object(e.try_into()?),
                Definition::SchemaExtension(e) => pub_schema.schema(e.try_into()?),
                Definition::ScalarTypeExtension(e) => pub_schema.scalar(e.try_into()?),
                Definition::ObjectTypeExtension(e) => pub_schema.object(e.try_into()?),
                Definition::InterfaceTypeExtension(e) => pub_schema.interface(e.try_into()?),
                Definition::UnionTypeExtension(e) => pub_schema.union(e.try_into()?),
                Definition::EnumTypeExtension(e) => pub_schema.enum_(e.try_into()?),
                Definition::InputObjectTypeExtension(e) => pub_schema.input_object(e.try_into()?),
            }
        }
        let mut compiler = ApolloCompiler::new();
        let sdl = pub_schema.to_string();
        compiler.add_type_system(&sdl, "public schema");
        let type_system = compiler.db.type_system();
        Ok(PublicGraph { type_system, sdl })
    }
}

#[derive(Clone)]
struct PublicGraph {
    type_system: Arc<TypeSystem>,
    sdl: String,
}

impl PublicGraph {
    fn validate_query(&self, operation: &str) -> Vec<ApolloDiagnostic> {
        let type_system = std::sync::Arc::clone(&self.type_system);
        let mut compiler = ApolloCompiler::new();
        compiler.add_executable(operation, "query.graphql");
        compiler.set_type_system_hir(type_system);
        compiler.validate()
    }
}

struct FeatureToggle {
    #[allow(dead_code)]
    configuration: Conf,
    raw_supergraph: Arc<RawSupergraph>,
    public_schema: Arc<PublicGraph>,
}

#[derive(Debug, Default, Deserialize, JsonSchema)]
struct Conf {
    // Put your plugin configuration here. It will automatically be deserialized from JSON.
    // Always put some sort of config here, even if it is just a bool to say that the plugin is enabled,
    // otherwise the yaml to enable the plugin will be confusing.
    message: String,
}

// This plugin is a skeleton for doing authentication that requires a remote call.
#[async_trait::async_trait]
impl Plugin for FeatureToggle {
    type Config = Conf;

    async fn new(init: PluginInit<Self::Config>) -> Result<Self, BoxError> {
        tracing::info!("{}", init.config.message);
        tracing::info!("{}", &init.supergraph_sdl);
        let raw_supergraph = RawSupergraph::new(init.supergraph_sdl.clone());
        let public_schema = raw_supergraph.for_features(Vec::new())?;

        Ok(FeatureToggle {
            configuration: init.config,
            raw_supergraph: Arc::new(raw_supergraph),
            public_schema: Arc::new(public_schema),
        })
    }

    fn supergraph_service(&self, service: supergraph::BoxService) -> supergraph::BoxService {
        let raw_supergraph = self.raw_supergraph.clone();
        let public_schema = self.public_schema.clone();

        ServiceBuilder::new()
            .checkpoint(move |req: supergraph::Request| {
                let to_use = if req.supergraph_request.headers().contains_key("feature") {
                    Arc::new(
                        raw_supergraph
                            .for_features(vec!["review-key".to_string()])
                            .unwrap(),
                    )
                } else {
                    public_schema.clone()
                };
                let query = req.supergraph_request.body().query.as_ref().unwrap();

                // if introspection then we must answer it ourselves
                let result = introspect::batch_introspect(
                    &to_use.sdl,
                    vec![query.to_string()],
                    QueryPlannerConfig::default(),
                );

                // return the result - for the moment (PoC), if the introspection result is not an error, we return it
                let introspection_result: Option<supergraph::Response> = match result.unwrap() {
                    Ok(mut r) => r.pop().and_then(|d| d.into_result().ok()).map(|data| {
                        supergraph::Response::builder()
                            .data(data)
                            .context(req.context.clone())
                            .build()
                            .unwrap()
                    }),
                    Err(_) => None,
                };

                if let Some(res) = introspection_result {
                    return Ok(ControlFlow::Break(res));
                }

                let errors = to_use.validate_query(query);
                let errors: Vec<_> = errors.iter().filter(|e| e.is_error()).collect();
                if !errors.is_empty() {
                    tracing::warn!(
                        "{}",
                        errors
                            .iter()
                            .fold("".to_string(), |acc, x| format!("{acc}\n{x}"))
                    );
                    // TODO: add all errors
                    let res: supergraph::Response = supergraph::Response::error_builder()
                        .error(
                            apollo_router::graphql::Error::builder()
                                .message(format!("{}", errors.get(0).unwrap()))
                                .extension_code("extension_code")
                                .build(),
                        )
                        .status_code(http::StatusCode::BAD_REQUEST)
                        .context(req.context.clone())
                        .build()
                        .expect("response is valid");
                    return Ok(ControlFlow::Break(res));
                }

                Ok(ControlFlow::Continue(req))
            })
            .service(service)
            .boxed()
    }
}

// This macro allows us to use it in our plugin registry!
// register_plugin takes a group name, and a plugin name.
register_plugin!("router_test", "feature_toggle", FeatureToggle);

#[cfg(test)]
mod tests {
    use super::*;
    use apollo_parser::Parser;
    use apollo_router::TestHarness;
    use indoc::indoc;
    use miette::Result;
    use pretty_assertions::assert_eq;
    use tower::BoxError;

    const SUPERGRAPH_SDL: &str = r#"
schema
  @link(url: "https://specs.apollo.dev/link/v1.0")
  @link(url: "https://specs.apollo.dev/join/v0.2", for: EXECUTION)
  @link(url: "https://myspecs.dev/myDirective/v1.0", import: ["@feature"])
{
  query: Query
}

directive @feature(name: String!) repeatable on ARGUMENT_DEFINITION | ENUM | ENUM_VALUE | FIELD_DEFINITION | INPUT_FIELD_DEFINITION | INPUT_OBJECT | INTERFACE | OBJECT | SCALAR | UNION

directive @join__field(graph: join__Graph!, requires: join__FieldSet, provides: join__FieldSet, type: String, external: Boolean, override: String, usedOverridden: Boolean) repeatable on FIELD_DEFINITION | INPUT_FIELD_DEFINITION

directive @join__graph(name: String!, url: String!) on ENUM_VALUE

directive @join__implements(graph: join__Graph!, interface: String!) repeatable on OBJECT | INTERFACE

directive @join__type(graph: join__Graph!, key: join__FieldSet, extension: Boolean! = false, resolvable: Boolean! = true) repeatable on OBJECT | INTERFACE | UNION | ENUM | INPUT_OBJECT | SCALAR

directive @link(url: String, as: String, for: link__Purpose, import: [link__Import]) repeatable on SCHEMA

scalar join__FieldSet

enum join__Graph {
  REVIEW @join__graph(name: "review", url: "http://localhost:9082/api/graphql")
  STATE @join__graph(name: "state", url: "http://localhost:9081/api/graphql")
}

scalar link__Import

enum link__Purpose {
  """
  `SECURITY` features provide metadata necessary to securely resolve fields.
  """
  SECURITY

  """
  `EXECUTION` features provide metadata necessary for operation execution.
  """
  EXECUTION
}

type Query
  @join__type(graph: REVIEW)
  @join__type(graph: STATE)
{
  reviews: [Review!]! @join__field(graph: REVIEW)
  states: [State!]! @join__field(graph: STATE)
}

type Review
  @join__type(graph: REVIEW)
{
  id: Int!
  key: String @feature(name: "review-key")
  state: State!
}

type State
  @join__type(graph: REVIEW, key: "id", extension: true)
  @join__type(graph: STATE, key: "id")
{
  id: Int!
  key: String! @join__field(graph: STATE)
  value: String! @join__field(graph: STATE)
}"#;

    const SUPERGRAPH_SDL_WITHOUT_FEATURES: &str = r#"
schema
  @link(url: "https://specs.apollo.dev/link/v1.0")
  @link(url: "https://specs.apollo.dev/join/v0.2", for: EXECUTION)
  @link(url: "https://myspecs.dev/myDirective/v1.0", import: ["@feature"])
{
  query: Query
}

directive @feature(name: String!) repeatable on ARGUMENT_DEFINITION | ENUM | ENUM_VALUE | FIELD_DEFINITION | INPUT_FIELD_DEFINITION | INPUT_OBJECT | INTERFACE | OBJECT | SCALAR | UNION

directive @join__field(graph: join__Graph!, requires: join__FieldSet, provides: join__FieldSet, type: String, external: Boolean, override: String, usedOverridden: Boolean) repeatable on FIELD_DEFINITION | INPUT_FIELD_DEFINITION

directive @join__graph(name: String!, url: String!) on ENUM_VALUE

directive @join__implements(graph: join__Graph!, interface: String!) repeatable on OBJECT | INTERFACE

directive @join__type(graph: join__Graph!, key: join__FieldSet, extension: Boolean! = false, resolvable: Boolean! = true) repeatable on OBJECT | INTERFACE | UNION | ENUM | INPUT_OBJECT | SCALAR

directive @link(url: String, as: String, for: link__Purpose, import: [link__Import]) repeatable on SCHEMA

scalar join__FieldSet

enum join__Graph {
  REVIEW @join__graph(name: "review", url: "http://localhost:9082/api/graphql")
  STATE @join__graph(name: "state", url: "http://localhost:9081/api/graphql")
}

scalar link__Import

enum link__Purpose {
  """
  `SECURITY` features provide metadata necessary to securely resolve fields.
  """
  SECURITY

  """
  `EXECUTION` features provide metadata necessary for operation execution.
  """
  EXECUTION
}

type Query
  @join__type(graph: REVIEW)
  @join__type(graph: STATE)
{
  reviews: [Review!]! @join__field(graph: REVIEW)
  states: [State!]! @join__field(graph: STATE)
}

type Review
  @join__type(graph: REVIEW)
{
  id: Int!
  state: State!
}

type State
  @join__type(graph: REVIEW, key: "id", extension: true)
  @join__type(graph: STATE, key: "id")
{
  id: Int!
  key: String! @join__field(graph: STATE)
  value: String! @join__field(graph: STATE)
}"#;

    #[tokio::test]
    async fn basic_test() -> Result<(), BoxError> {
        let _ = TestHarness::builder()
            .configuration_json(serde_json::json!({
                "plugins": {
                    "router_test.feature_toggle": {
                        "message" : "Starting my plugin"
                    }
                }
            }))
            .unwrap()
            .build_supergraph()
            .await
            .unwrap();
        // let request = supergraph::Request::canned_builder().build().unwrap();
        // let mut streamed_response = test_harness.oneshot(request).await?;
        //
        // let first_response = streamed_response
        //     .next_response()
        //     .await
        //     .expect("couldn't get primary response");
        //
        // assert!(first_response.data.is_some());
        //
        // println!("first response: {:?}", first_response);
        // let next = streamed_response.next_response().await;
        // println!("next response: {:?}", next);
        //
        // // You could keep calling .next_response() until it yields None if you're expexting more parts.
        // assert!(next.is_none());
        Ok(())
    }

    #[test]
    fn test_creating_public_schema() {
        let raw_supergraph_schema = RawSupergraph::new(Arc::new(SUPERGRAPH_SDL.to_string()));
        let public_schema = raw_supergraph_schema.for_features(Vec::new()).unwrap();
        let expected_schema: apollo_encoder::Document =
            Parser::new(SUPERGRAPH_SDL_WITHOUT_FEATURES)
                .parse()
                .document()
                .try_into()
                .unwrap();
        assert_eq!(
            expected_schema.to_string(),
            public_schema
                .sdl
                .to_string()
                .replace("\n   @join", " @join") // hack to remove formatting differences
        );
    }

    #[test]
    fn test_creating_public_schema_2() {
        let sdl = indoc! {r#"
            type Review {
              id: Int!
              key: String @feature(name: "review-key")
            }

            type Query {
              reviews: [Review!]!
            }
        "#};
        let raw_supergraph_schema = RawSupergraph::new(Arc::new(sdl.to_string()));
        let public_schema = raw_supergraph_schema.for_features(Vec::new()).unwrap();

        assert_eq!(
            &public_schema.sdl,
            indoc! {r#"
                type Review {
                  id: Int!
                }
                type Query {
                  reviews: [Review!]!
                }
            "#}
        );

        assert_no_errors(public_schema.validate_query(indoc! {r#"
            {
              reviews {
                id2
              }
            }
        "#}));

        let errors = public_schema.validate_query(indoc! {r#"
            {
              reviews {
                id
                key
              }
            }
        "#});
        // https://github.com/apollographql/apollo-rs/issues/392
        //assert_ne!(errors.len(), 0);
    }

    #[test]
    fn test_closed_beta_schema() {
        let sdl = indoc! {r#"
            type Review {
              id: Int!
              key: String @feature(name: "review-key")
            }

            type Query {
              reviews: [Review!]!
            }
        "#};
        let raw_supergraph_schema = RawSupergraph::new(Arc::new(sdl.to_string()));
        let public_schema = raw_supergraph_schema
            .for_features(vec!["review-key".to_string()])
            .unwrap();

        assert_eq!(
            &public_schema.sdl,
            indoc! {r#"
                type Review {
                  id: Int!
                  key: String @feature(name: "review-key")
                }
                type Query {
                  reviews: [Review!]!
                }
            "#}
        );

        assert_no_errors(public_schema.validate_query(indoc! {r#"
            {
              reviews {
                id
                key
              }
            }
        "#}));
    }

    #[test]
    fn issue_with_apollo_compiler() {
        // https://github.com/apollographql/apollo-rs/issues/392
        let schema = indoc! {r#"
            type Review {
              id: Int!
            }

            type Query {
              reviews: [Review!]!
            }
        "#};

        let query = indoc! {r#"
            {
              reviews {
                id
                key
              }
            }
        "#};

        let failing_query = indoc! {r#"
            {
              revies {
                id
                key
              }
            }
        "#};

        let mut compiler = ApolloCompiler::new();
        compiler.add_type_system(schema, "schema");

        let id = compiler.add_executable(failing_query, "query");
        let diagnostics = compiler.validate();
        for diagnostic in &diagnostics {
            println!("{}", diagnostic);
        }
        assert!(!diagnostics.is_empty());

        compiler.update_executable(id, query);
        assert!(compiler.validate().is_empty());
    }

    fn assert_no_errors(errors: Vec<ApolloDiagnostic>) {
        assert_eq!(
            errors.len(),
            0,
            "Errors: {}",
            errors
                .iter()
                .fold("".to_string(), |acc, x| format!("{}\n{}", acc, x))
        );
    }
}
