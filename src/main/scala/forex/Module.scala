package forex

import cats.effect.{Concurrent, Timer}
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}
import org.typelevel.log4cats.Logger

class Module[F[_] : Logger : Concurrent : Timer](config: ApplicationConfig, httpClient: Client[F]) {

  private val ratesClient: RatesClient[F] = RatesClient.rateClient[F](config, httpClient)
  private val ratesService: RatesService[F] = RatesServices.cachedClient[F](config, ratesClient)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

  def initCache = ratesService.init()
}
