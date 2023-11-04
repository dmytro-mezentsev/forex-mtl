package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxEitherId, toFunctorOps}
import forex.config.OneFrameConfig
import forex.domain._
import forex.services.rates.Algebra
import forex.services.rates.Converters.ExchangeRateOps
import forex.services.rates.Protocol.ExchangeRate
import forex.services.rates.errors._
import org.http4s.Method.GET
import org.http4s._
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class OneFrameClient[F[_] : Sync](oneFameConfig: OneFrameConfig, httpClient: Client[F]) extends Algebra[F] {

  implicit def logger = Slf4jLogger.getLogger[F]

  override def get(pairs: List[Rate.Pair]): F[Either[Error, List[Rate]]] = {
    val queryParameters = pairs.map(pair => "pair=" + pair.from + pair.to).mkString("&")
    val uri = Uri.fromString(s"http://${oneFameConfig.host}:${oneFameConfig.port}/rates?$queryParameters").getOrElse(Uri(path = "/"))
    val request = Request[F](GET, uri, headers = Headers.of(Header("token", oneFameConfig.token)))

    httpClient.expect[List[ExchangeRate]](request).attempt
      .map {
      case Right(exchangeRate) =>
        exchangeRate.map(_.toRate).asRight[Error]
      case Left(error) =>
        Logger[F].error(s"OneFrame request failed with error: ${error.getMessage}")
        Error.OneFrameLookupFailed(error.getMessage).asLeft[List[Rate]]
    }
  }
}
