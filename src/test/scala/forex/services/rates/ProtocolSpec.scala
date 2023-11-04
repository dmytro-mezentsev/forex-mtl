package forex.services.rates

import io.circe.jawn
import java.time.LocalDateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProtocolSpec extends AnyFlatSpec with Matchers {

  import Protocol._

  val exchangeRateJson =
    """
      {
          "from": "USD",
          "to": "JPY",
          "bid": 0.6118225421857174,
          "ask": 0.8243869101616611,
          "price": 0.71810472617368925,
          "time_stamp": "2023-11-03T19:44:16.842Z"
      }
    """

  val exchangeRateListJson =
    """[
        {
            "from": "EUR",
            "to": "USD",
            "bid": 0.6118225421857174,
            "ask": 0.8243869101616611,
            "price": 0.71810472617368925,
            "time_stamp": "2023-11-04T10:57:26.041Z"
        },
        {
            "from": "USD",
            "to": "JPY",
            "bid": 0.8435259660697864,
            "ask": 0.4175532166907524,
            "price": 0.6305395913802694,
            "time_stamp": "2023-11-04T10:57:26.041Z"
        }
    ]"""

  "exchangeRateDecoder" should "decode JSON to ExchangeRate" in {
    val result = jawn.decode[ExchangeRate](exchangeRateJson)
    val expected = ExchangeRate("USD", "JPY", 0.6118225421857174, 0.8243869101616611, 0.71810472617368925, LocalDateTime.parse("2023-11-03T19:44:16.842Z", formatter))

    result should be(Right(expected))
  }

  "exchangeRateEntityDecoder" should "decode JSON to List of ExchangeRate" in {
    val result = jawn.decode[List[ExchangeRate]](exchangeRateListJson)
    val expected = List(
      ExchangeRate("EUR", "USD", 0.6118225421857174, 0.8243869101616611, 0.71810472617368925, LocalDateTime.parse("2023-11-04T10:57:26.041Z", formatter)),
      ExchangeRate("USD", "JPY", 0.8435259660697864, 0.4175532166907524, 0.6305395913802694, LocalDateTime.parse("2023-11-04T10:57:26.041Z", formatter))
    )

    result should be(Right(expected))
  }
}
