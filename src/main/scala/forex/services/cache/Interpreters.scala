package forex.services.cache

import cats.effect.{Concurrent, Timer}
import forex.config.ApplicationConfig
import forex.services.RatesClient
import forex.services.cache.interpreters.RateCache
import org.typelevel.log4cats.Logger

object Interpreters {

  def cachedClient[F[_] : Logger : Timer : Concurrent](config: ApplicationConfig, ratesClient: RatesClient[F]): Algebra[F] =
    new RateCache[F](ratesClient, config.oneFrame)
}
