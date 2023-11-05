package forex.services.rates.interpreters

import cats.effect.{IO, Timer}
import forex.config.{OneFrameConfig, OneFrameHttpConfig}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.errors.Error.OneFrameLookupFailed
import io.circe.{Json, jawn}
import java.time.OffsetDateTime
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.{Header, HttpApp, Response}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.DurationInt

class OneFrameClientSpec extends AnyFlatSpec with Matchers with Http4sDsl[IO] {
  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)
  implicit val logger = Slf4jLogger.getLogger[IO]

  object PairQueryParameter extends QueryParamDecoderMatcher[String]("pair")

  val mockHttpClient = Client.fromHttpApp(HttpApp[IO] {
    case GET -> Root / "rates" :? PairQueryParameter(pair) =>
      if (pair == "USDJPY") {
        val body = jawn.parse("""[{"from":"USD","to":"JPY","bid":0.5493447192669441,"ask":0.3908646611051285,"price":0.4701046901860363,"time_stamp":"2023-11-04T17:20:21.188Z"}]""")
          .getOrElse(Json.arr())
        IO.delay(Response[IO](Ok).withEntity(body).withHeaders(Header("Content-Type", "application/json")))
      } else {
        IO.delay(Response[IO](BadRequest).withHeaders(Header("Content-Type", "application/json")))
      }
  })

  val oneFrameConfig = OneFrameConfig(OneFrameHttpConfig("localhost", 8080, "your-token"), 5.minutes, 2, 1.seconds)

  "OneFrameClient" should "return a list of rates when the HTTP request is successful" in {
    val client = new OneFrameClient[IO](oneFrameConfig, mockHttpClient)
    val pairs = List(Rate.Pair(Currency.USD, Currency.JPY))
    val result = client.get(pairs).unsafeRunSync()

    val expectedRate = Rate(Rate.Pair(Currency.USD, Currency.JPY), Price(BigDecimal(0.4701046901860363)), Timestamp(OffsetDateTime.parse("2023-11-04T17:20:21.188Z")))
    result shouldBe Right(List(expectedRate))
  }

  val mockHttpClientWrongHeader = Client.fromHttpApp(HttpApp[IO] {
    case GET -> Root / "rates"  =>
      val body = jawn.parse("""{"error": "Forbidden"}""".stripMargin)
        .getOrElse(Json.arr())
      IO.delay(Response[IO](Ok).withEntity(body).withHeaders(Header("Content-Type", "application/json")))
  })

  "OneFrameClient" should "return Error with wrong token" in {
    val client = new OneFrameClient[IO](oneFrameConfig, mockHttpClientWrongHeader)
    val pairs = List(Rate.Pair(Currency.USD, Currency.JPY))
    val result = client.get(pairs).unsafeRunSync()
    val expectedError = Left(OneFrameLookupFailed("Invalid message body: Could not decode JSON: {\n  \"error\" : \"Forbidden\"\n}"))

    result shouldBe expectedError
  }
}
