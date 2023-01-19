import cats.effect.{Deferred, IO, Resource}
import eu.monniot.process.Process
import weaver._

import scala.concurrent.duration._

object ExampleSpec extends SimpleIOSuite {

  override def sharedResource =
    for {
      stateRun <- state.Main.run.background
      reviewRun <- review.Main.run.background
    } yield (stateRun, reviewRun)

  private def routerResource(log: Log[IO]) = Process
    .spawn[IO]("bash", "-c", "./example/router/start.sh")
    .flatMap { p =>
      Resource.make(p.pid.map(pid => (p, pid))) { case (_, pid) =>
        for {
          _ <- log.info("killing router process (child of 'start' bash script)")
          _ <- IO.blocking(sys.process.Process("killall" :: "router" :: Nil).!)
        } yield ()
      }
    }

  test("examples can be federated") { (_, log) =>
    log.info("starting router") >> routerResource(log).use { case (process, _) =>
      for {
        graphqlEndpointExposed <- Deferred[IO, Boolean]
        _ <- process.stdout
          .through(fs2.text.utf8.decode)
          .evalTap { s =>
            log.debug(s) >> {
              if (s.contains("GraphQL endpoint exposed")) {
                graphqlEndpointExposed.complete(true) >> process.terminate()
              } else IO.unit
            }
          }
          .compile
          .drain
          .start
        _ <- process.stderr
          .through(fs2.text.utf8.decode)
          .evalTap(s => log.error(s))
          .compile
          .drain
          .start
        exposed <- graphqlEndpointExposed.get.timeout(20.seconds)
      } yield expect(exposed)
    }
  }
}
