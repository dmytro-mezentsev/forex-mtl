package forex.http.rates

import forex.domain.Currency
import forex.http.rates.Protocol.ApiError
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import scala.util.Try

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Either[ApiError, Currency]] =
    QueryParamDecoder[String].map { str =>
      Try(Currency.fromString(str)).toEither.left.map(_ => ApiError(s"Invalid currency: $str"))
    }
  object FromQueryParam extends QueryParamDecoderMatcher[Either[ApiError, Currency]]("from")
  object ToQueryParam extends QueryParamDecoderMatcher[Either[ApiError, Currency]]("to")

}
