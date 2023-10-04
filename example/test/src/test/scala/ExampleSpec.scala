import cats.effect.{Deferred, IO, Resource}
import eu.monniot.process.Process
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{Logger, LoggerFactory, SelfAwareStructuredLogger}
import weaver._

import scala.concurrent.duration._

object ExampleSpec extends SimpleIOSuite {

  private def resources(log: Log[IO]): Resource[IO, Unit] = {
    val l = logger(log)
    // TODO: use log
    implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    for {
      _ <- state.Main.run().background
      _ <- review.Main.run().background
    } yield ()
  }

  private def routerResource(log: Log[IO]): Resource[IO, Process[IO]] = Process
    .spawn[IO]("bash", "-c", "./example/router/start.sh")
    .flatMap { p =>
      Resource.make(IO(p))(_ => killRouter(log))
    }

  private def killRouter(log: Log[IO]): IO[Unit] = for {
    _ <- log.info("killing router process (child of 'start' bash script)")
    _ <- IO
      .blocking(sys.process.Process("killall" :: "router" :: Nil).!)
      .onError(e => log.warn("failed to kill router process", cause = e))
  } yield ()

  test("examples can be federated") { (_, log) =>
    val allResources = for {
      _ <- resources(log)
      router <- routerResource(log)
    } yield router

    allResources.use { router =>
      for {
        graphqlEndpointExposed <- Deferred[IO, Boolean]
        _ <- router.stdout
          .through(fs2.text.utf8.decode)
          .evalTap { s =>
            log.debug(s) >> {
              if (s.contains("GraphQL endpoint exposed")) {
                graphqlEndpointExposed.complete(true) >> router.terminate()
              } else IO.unit
            }
          }
          .compile
          .drain
          .start
        _ <- router.stderr
          .through(fs2.text.utf8.decode)
          .evalTap(s => log.error(s))
          .compile
          .drain
          .start
        exposed <- graphqlEndpointExposed.get.timeout(30.seconds)
      } yield expect(exposed)
    }
  }

  private def logger(log: Log[IO]): Logger[IO] = new Logger[IO] {
    override def error(t: Throwable)(message: => String): IO[Unit] = log.error(message, cause = t)
    override def warn(t: Throwable)(message: => String): IO[Unit] = log.warn(message, cause = t)
    override def info(t: Throwable)(message: => String): IO[Unit] = log.info(message, cause = t)
    override def debug(t: Throwable)(message: => String): IO[Unit] = log.debug(message, cause = t)
    override def trace(t: Throwable)(message: => String): IO[Unit] = log.debug(message, cause = t)
    override def error(message: => String): IO[Unit] = log.error(message)
    override def warn(message: => String): IO[Unit] = log.warn(message)
    override def info(message: => String): IO[Unit] = log.info(message)
    override def debug(message: => String): IO[Unit] = log.debug(message)
    override def trace(message: => String): IO[Unit] = log.debug(message)
  }
}
