package forex.services.rates.interpreters

import cats.effect.{Sync, Timer}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxApplicativeId, catsSyntaxEitherId, catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps}
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
import scala.concurrent.duration.FiniteDuration

class OneFrameClient[F[_] : Sync : Timer : Logger](oneFameConfig: OneFrameConfig, httpClient: Client[F]) extends Algebra[F] {


  override def get(pairs: List[Rate.Pair]): F[Either[Error, List[Rate]]] = retryableGetRates(pairs, oneFameConfig.retryCount, oneFameConfig.retryDelay)

  private def retryableGetRates(pairs: List[Rate.Pair], retriesLeft: Int, delay: FiniteDuration): F[Either[Error, List[Rate]]] = {
    getRates(pairs).flatMap {
      case Right(rates) => rates.asRight[Error].pure[F]
      case Left(_) if retriesLeft > 0 =>
        Logger[F].warn(s"Retrying, $retriesLeft retries left...") >>
        Timer[F].sleep(delay) >>
        retryableGetRates(pairs, retriesLeft - 1, delay)
      case Left(error) =>
        Logger[F].error(s"Retries exhausted, returning error: ${error}")
        error.asLeft[List[Rate]].pure[F]
    }
  }


  private def getRates(pairs: List[Rate.Pair]): F[Either[Error, List[Rate]]] = {
    val queryParameters = pairs.map(pair => "pair=" + pair.from + pair.to).mkString("&")
    val uri = Uri.fromString(s"http://${oneFameConfig.http.host}:${oneFameConfig.http.port}/rates?$queryParameters").getOrElse(Uri(path = "/"))
    val request = Request[F](GET, uri, headers = Headers.of(Header("token", oneFameConfig.http.token)))

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
