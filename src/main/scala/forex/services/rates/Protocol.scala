package forex.services.rates

import cats.effect.Sync
import cats.implicits.toBifunctorOps
import io.circe.{Decoder, HCursor}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.http4s.EntityDecoder
import org.http4s.circe.CirceEntityDecoder
import scala.util.Try

object Protocol {

  case class ExchangeRate(
    from: String,
    to: String,
    bid: Double,
    ask: Double,
    price: Double,
    timeStamp: LocalDateTime
  )
  protected[rates] val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  implicit val dateDecoder = Decoder.decodeString.emap[LocalDateTime](str => {
    Try(LocalDateTime.parse(str, formatter)).toEither.leftMap(_.getMessage)
  })

  implicit val exchangeRateDecoder: Decoder[ExchangeRate] = (c: HCursor) => {
    for {
      from <- c.downField("from").as[String]
      to <- c.downField("to").as[String]
      bid <- c.downField("bid").as[Double]
      ask <- c.downField("ask").as[Double]
      price <- c.downField("price").as[Double]
      timeStamp <- c.downField("time_stamp").as[LocalDateTime]
    } yield ExchangeRate(from, to, bid, ask, price, timeStamp)
  }

  implicit def exchangeRateEntityDecoder[F[_] : Sync]: EntityDecoder[F, List[ExchangeRate]] =
    CirceEntityDecoder.circeEntityDecoder
}
