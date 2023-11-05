package forex.http
package rates

import cats.effect.Sync
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_] : Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromE) +& ToQueryParam(toE) =>
      (for {
        from <- Sync[F].fromEither(fromE)
        to <- Sync[F].fromEither(toE)
        _ <- if (to == from) Sync[F].raiseError[Unit](ApiError("From and to currencies must be different")) else Sync[F].unit
      } yield from -> to)
        .flatMap { case (from, to) =>
          rates.get(RatesProgramProtocol.GetRatesRequest(from, to))
        }
        .flatMap {
          case Right(rate) => Ok(rate.asGetApiResponse)
          case Left(error) => BadRequest(ApiError(error.getMessage))
        }.handleErrorWith {
        case ApiError(error) => BadRequest(ApiError(error))
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
