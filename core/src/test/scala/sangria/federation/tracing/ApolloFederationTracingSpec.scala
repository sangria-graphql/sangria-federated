package sangria.federation.tracing

import com.google.protobuf.timestamp.Timestamp
import io.circe.{Json, JsonObject, parser}
import io.circe.parser.parse
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import reports.Trace
import sangria.execution.Executor
import sangria.macros.LiteralGraphQLStringContext
import sangria.marshalling.ScalaInput
import sangria.marshalling.circe._

import java.util.Base64

class ApolloFederationTracingSpec extends AsyncWordSpec with Matchers with OptionValues {
  import sangria.federation.TestSchema._

  private val mainQuery =
    gql"""
      query Foo {
        friends {
          ...Name
          ...Name2
        }
      }

      query Test($$limit: Int!) {
        __typename
        name
             ...Name1
             pets(limit: $$limit) {
          ... on Cat {
            name
            meows
            ...Name
          }
          ... on Dog {
            ...Name1
            ...Name1
            foo: name
            barks
          }
        }
      }

      fragment Name on Named {
        name
        ...Name1
      }

      fragment Name1 on Named {
        ... on Person {
          name
        }
      }

      fragment Name2 on Named {
        name
      }
    """

  "ApolloFederationTracing" should {
    "add tracing extension" in {
      val vars = ScalaInput.scalaInput(Map("limit" -> 4))

      Executor
        .execute(
          schema,
          mainQuery,
          root = bob,
          operationName = Some("Test"),
          variables = vars,
          middleware = ApolloFederationTracing :: Nil)
        .map { (result: Json) =>
          result.hcursor.get[Json]("data") should be(parse("""
            {
              "__typename": "Person",
              "name": "Bob",
              "pets": [
                {
                  "name": "Garfield",
                  "meows": false
                },
                {
                  "name": "Garfield",
                  "meows": false
                },
                {
                  "name": "Garfield",
                  "meows": false
                },
                {
                  "name": "Garfield",
                  "meows": false
                }
              ]
            }
          """))

          val ftv1Trace =
            parseTrace(result.hcursor.downField("extensions").get[String]("ftv1").toOption.value)

          // TODO: add root node and fields
          removeTime(ftv1Trace) should be(
            Trace(
              startTime = Some(startTimestamp),
              endTime = Some(endTimestamp),
              durationNs = 1
            ))
        }
    }
  }

  private def parseTrace(trace: String): Trace = Trace.parseFrom(Base64.getDecoder.decode(trace))

  private val startTimestamp: Timestamp = Timestamp(1, 0)
  private val endTimestamp: Timestamp = Timestamp(1, 1)

  private def removeTime(trace: Trace): Trace =
    trace.update(
      _.startTime := startTimestamp,
      _.endTime := endTimestamp,
      _.durationNs := 1
    )
}
