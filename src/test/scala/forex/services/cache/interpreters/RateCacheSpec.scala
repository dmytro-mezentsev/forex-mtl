package forex.services.cache.interpreters

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import forex.config.OneFrameConfig
import forex.domain._
import forex.services.RatesClient
import forex.services.rates.errors
import java.time.OffsetDateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.DurationInt

class RateCacheSpec extends AnyFlatSpec with Matchers {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
  implicit val logger = Slf4jLogger.getLogger[IO]
  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)

  private val pairUsdJpy: Rate.Pair = Rate.Pair(Currency.USD, Currency.JPY)
  private val pairEurCad: Rate.Pair = Rate.Pair(Currency.EUR, Currency.CAD)
  private val pairJpyAud: Rate.Pair = Rate.Pair(Currency.JPY, Currency.AUD)
  val rateUsdJpy = Rate(pairUsdJpy, Price(0.9), Timestamp(OffsetDateTime.now()))
  val rateEurCad = Rate(pairEurCad, Price(0.8), Timestamp(OffsetDateTime.now()))
  val rateJpyAud = Rate(pairJpyAud, Price(0.7), Timestamp(OffsetDateTime.now()))

  val mockRatesClient = new RatesClient[IO]() {
    override def get(pairs: List[Rate.Pair]): IO[Either[errors.Error, List[Rate]]] = {
      List(rateUsdJpy, rateEurCad, rateJpyAud).asRight[errors.Error].pure[IO]
    }
  }

  "RateCache" should "initialize the cache" in {
    val oneFrameConfig = OneFrameConfig("localhost", 8080, "token", 5.seconds)
    val rateCache = new RateCache[IO](mockRatesClient, oneFrameConfig)
    val result = rateCache.init().unsafeRunSync()

    result should be(Right(()))
    rateCache.get(List(pairUsdJpy)).unsafeRunSync() shouldBe Right(List(rateUsdJpy))
    rateCache.get(List(pairUsdJpy, pairEurCad, pairJpyAud)).unsafeRunSync() shouldBe Right(List(rateUsdJpy, rateEurCad, rateJpyAud))
  }

}
