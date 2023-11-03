package forex

import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.ExecutionContext

object Main extends IOApp {
  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    val program = for {
      _ <- logger.info("Starting the Forex application")
      exitCode <- new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)
      _ <- logger.info("Shutting down the Forex application")
    } yield exitCode

    program
  }

}

class Application[F[_] : ConcurrentEffect : Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      module = new Module[F](config)
      _ <- BlazeServerBuilder[F](ec)
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(module.httpApp)
        .serve
    } yield ()

}
