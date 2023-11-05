package forex

import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.ExecutionContext

object Main extends IOApp {
  implicit val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    val program = for {
      _ <- logger.info("Starting the Forex application")
      exitCode <- new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)
      _ <- logger.info("Shutting down the Forex application")
    } yield exitCode

    program
  }

}

class Application[F[_] : Logger : ConcurrentEffect : Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      httpClients <- BlazeClientBuilder[F](ec).stream
      module = new Module[F](config, httpClients)
      _ <- Stream.eval(module.initCache)
      _ <- BlazeServerBuilder[F](ec)
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(module.httpApp)
        .serve
    } yield ()

}
