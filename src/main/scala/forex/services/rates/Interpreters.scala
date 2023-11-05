package forex.services.rates

import cats.effect.{Sync, Timer}
import forex.config.ApplicationConfig
import forex.services.rates.interpreters._
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

object Interpreters {
  def rateClient[F[_] : Sync : Timer : Logger](config: ApplicationConfig, httpClient: Client[F]): Algebra[F] =
    new OneFrameClient[F](config.oneFrame, httpClient)
}
