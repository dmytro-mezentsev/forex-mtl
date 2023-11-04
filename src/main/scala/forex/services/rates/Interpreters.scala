package forex.services.rates

import cats.effect.Sync
import forex.config.ApplicationConfig
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_] : Sync](config: ApplicationConfig, httpClient: Client[F]): Algebra[F] =
    new OneFrameClient[F](config.oneFrame, httpClient)
}
