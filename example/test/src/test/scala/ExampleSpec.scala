import cats.effect.{Deferred, IO, Resource}
import eu.monniot.process.Process
import org.typelevel.log4cats.Logger
import weaver._

import scala.concurrent.duration._

object ExampleSpec extends SimpleIOSuite {

  private def resources(log: Log[IO]): Resource[IO, Process[IO]] = {
    val l = logger(log)
    for {
      _ <- state.Main.run(l).background
      _ <- review.Main.run(l).background
      router <- routerResource(log)
    } yield router._1
  }

  private def routerResource(log: Log[IO]): Resource[IO, (Process[IO], Int)] = Process
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
    resources(log).use { router =>
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
