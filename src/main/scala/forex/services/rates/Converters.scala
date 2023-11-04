package forex.services.rates

import forex.domain._
import forex.services.rates.Protocol.ExchangeRate

object Converters {

  private[rates] implicit class ExchangeRateOps(val exchangeRate: ExchangeRate) extends AnyVal {

    import exchangeRate._

    def toRate: Rate = Rate(
      pair = Rate.Pair(Currency.fromString(from), Currency.fromString(to)),
      price = Price(price),
      timestamp = Timestamp(timeStamp.atOffset(java.time.ZoneOffset.UTC))
    )
  }
}
