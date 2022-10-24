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
use tower::BoxError;
use tower::ServiceBuilder;
use tower::ServiceExt;

const SUPERGRAPH1_SDL: &str = r#"
schema
  @link(url: "https://specs.apollo.dev/link/v1.0")
  @link(url: "https://specs.apollo.dev/join/v0.2", for: EXECUTION)
{
  query: Query
}

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
}
"#;

const SUPERGRAPH2_SDL: &str = r#"
schema
  @link(url: "https://specs.apollo.dev/link/v1.0")
  @link(url: "https://specs.apollo.dev/join/v0.2", for: EXECUTION)
{
  query: Query
}

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
  key: String
  state: State!
}

type State
  @join__type(graph: REVIEW, key: "id", extension: true)
  @join__type(graph: STATE, key: "id")
{
  id: Int!
  key: String! @join__field(graph: STATE)
  value: String! @join__field(graph: STATE)
}
"#;

#[derive(Debug)]
struct FeatureToggle {
    #[allow(dead_code)]
    configuration: Conf,
    #[allow(dead_code)]
    supergraph_sdl: std::sync::Arc<String>,
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
        // tracing::info!("{}", &init.supergraph_sdl);
        let parser = apollo_parser::Parser::new(&init.supergraph_sdl);
        let ast = parser.parse();
        // ast.document().definitions().
        let sdl = ast.document().to_string();
        tracing::info!("{:?}", &sdl);
        // let configuration = apollo_router::Configuration::default();
        // apollo_router::spec::Schema::parse(&init.supergraph_sdl, &configuration).unwrap();

        Ok(FeatureToggle {
            configuration: init.config,
            supergraph_sdl: init.supergraph_sdl,
        })
    }

    fn supergraph_service(&self, service: supergraph::BoxService) -> supergraph::BoxService {
        let handler = move |request: supergraph::Request| {
            // PoC limitation: choose between 2 constant schemas
            let supergraph_sdl = schema_sdl(&request);
            tracing::info!("{:?}", &request.supergraph_request);

            let query = &request
                .supergraph_request
                .body()
                .query
                .as_ref()
                .unwrap()
                .to_string();
            // if introspection then
            let result = introspect::batch_introspect(
                supergraph_sdl.as_ref(),
                vec![query.to_string()],
                QueryPlannerConfig::default(),
            );
            tracing::info!("{:?}", &result);
            // return the result - for the moment (PoC), if the introspection result is not an error, we return it
            let introspection_result: Option<supergraph::Response> = match result.unwrap() {
                Ok(mut r) => r.pop().and_then(|d| d.into_result().ok()).map(|data| {
                    supergraph::Response::builder()
                        .data(data)
                        .context(request.context.clone())
                        .build()
                        .unwrap()
                }),
                Err(_) => None,
            };

            async {
                match introspection_result {
                    Some(res) => Ok(ControlFlow::Break(res)),
                    None => Ok(ControlFlow::Continue(request)),
                }
            }
        };

        ServiceBuilder::new()
            .checkpoint_async(handler)
            .buffered()
            .service(service)
            .boxed()
    }
}

fn schema_sdl(request: &supergraph::Request) -> String {
    if request.supergraph_request.headers().contains_key("feature") {
        SUPERGRAPH2_SDL.to_string()
    } else {
        SUPERGRAPH1_SDL.to_string()
    }
}

// This macro allows us to use it in our plugin registry!
// register_plugin takes a group name, and a plugin name.
register_plugin!("router_test", "feature_toggle", FeatureToggle);

#[cfg(test)]
mod tests {
    use apollo_router::services::supergraph;
    use apollo_router::TestHarness;
    use tower::BoxError;
    use tower::ServiceExt;

    #[tokio::test]
    async fn basic_test() -> Result<(), BoxError> {
        let test_harness = TestHarness::builder()
            .configuration_json(serde_json::json!({
                "plugins": {
                    "router_test.feature_toggle": {
                        "message" : "Starting my plugin"
                    }
                }
            }))
            .unwrap()
            .build()
            .await
            .unwrap();
        let request = supergraph::Request::canned_builder().build().unwrap();
        let mut streamed_response = test_harness.oneshot(request).await?;

        let first_response = streamed_response
            .next_response()
            .await
            .expect("couldn't get primary response");

        assert!(first_response.data.is_some());

        println!("first response: {:?}", first_response);
        let next = streamed_response.next_response().await;
        println!("next response: {:?}", next);

        // You could keep calling .next_response() until it yields None if you're expexting more parts.
        assert!(next.is_none());
        Ok(())
    }
}
