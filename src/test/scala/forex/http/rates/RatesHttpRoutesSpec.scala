package forex.http.rates

import cats.effect.IO
import forex.domain.{Price, Rate, Timestamp}
import forex.http.jsonDecoder
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import io.circe.Json
import io.circe.syntax._
import java.time.OffsetDateTime
import org.http4s._
import org.http4s.circe.CirceEntityEncoder
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RatesHttpRoutesSpec extends AnyFlatSpec with Matchers {
  implicit val circeEntityEncoder: EntityEncoder[IO, Json] = CirceEntityEncoder.circeEntityEncoder
  implicit val ratesProgram: RatesProgram[IO] = new MockRatesProgram

  val httpRoutes = new RatesHttpRoutes[IO](ratesProgram)

  val rate = 0.9244
  val timestamp = Timestamp(OffsetDateTime.now())

  class MockRatesProgram extends RatesProgram[IO] {
    override def get(request: GetRatesRequest): IO[Right[Nothing, Rate]] = {
      IO(Right(Rate(Rate.Pair(request.from, request.to), Price(BigDecimal(rate)), timestamp)))
    }
  }

  it should "return a valid response for a valid request" in {
    val validRequest = Request[IO](Method.GET, uri"/rates?from=USD&to=JPY")
    val expectedValidResponse = s"""{"from":"USD","to":"JPY","price":$rate,"timestamp":"${timestamp.value.toString}"}"""

    val response = httpRoutes.routes.run(validRequest).value.unsafeRunSync().getOrElse(Response.notFound)
    response.status shouldEqual Status.Ok
    response.as[String].unsafeRunSync().replaceAll("\t", "") shouldEqual expectedValidResponse
  }

  it should "return an error response for same values `from` and `to`" in {
    val invalidRequest = Request[IO](Method.GET, uri"/rates?from=USD&to=USD")
    val expectedInvalidResponse = Json.obj("error" -> "From and to currencies must be different".asJson)

    val response = httpRoutes.routes.run(invalidRequest).value.unsafeRunSync().getOrElse(Response.notFound)
    response.status shouldEqual Status.BadRequest
    response.as[Json].unsafeRunSync() shouldEqual expectedInvalidResponse
  }

  it should "return an error response for invalid currency value" in {
    val invalidRequest = Request[IO](Method.GET, uri"/rates?from=USD&to=123")
    val expectedInvalidResponse = Json.obj("error" -> "Invalid currency: 123".asJson)

    val response = httpRoutes.routes.run(invalidRequest).value.unsafeRunSync().getOrElse(Response.notFound)
    response.status shouldEqual Status.BadRequest
    response.as[Json].unsafeRunSync() shouldEqual expectedInvalidResponse
  }

}
