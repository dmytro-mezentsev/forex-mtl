package forex.services.cache.interpreters

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Timer}
import cats.implicits._
import forex.config.OneFrameConfig
import forex.domain._
import forex.services.RatesClient
import forex.services.cache.Algebra
import forex.services.rates.errors.Error
import org.typelevel.log4cats.Logger
import scala.concurrent.duration.FiniteDuration

class RateCache[F[_] : Logger : Timer : Concurrent](service: RatesClient[F], oneFameConfig: OneFrameConfig) extends Algebra[F] {

  private val supportedPairs = Currency.values.flatMap(from => Currency.values.collect { case to if from != to => Rate.Pair(from, to) })
  private val cachedRatesMap: Ref[F, Map[Currency, Map[Currency, Rate]]] = Ref.unsafe(Map.empty)

  override def init(): F[Either[Error, Unit]] = {
    Logger[F].info("Initializing RateCache")
    initializeCacheUpdate(oneFameConfig.cacheUpdateInterval)
  }

  override def get(pairs: List[Rate.Pair]): F[Either[Error, List[Rate]]] = {
    cachedRatesMap.get.map { cache =>
      pairs.map(pair => cache(pair.from)(pair.to)).asRight[Error]
    }
  }

  private def updateCache(): F[Unit] = {
    service.get(supportedPairs.toList).flatMap {
      case Right(rates) =>
        val newRates = rates.groupBy(_.pair.from).view.mapValues(_.groupBy(_.pair.to).view.mapValues(_.head).toMap).toMap
        cachedRatesMap.set(newRates)
      case Left(error) =>
        Logger[F].error(s"RateCache update failed with error: $error")
    }
  }


  private def initializeCacheUpdate(interval: FiniteDuration): F[Error Either Unit] = {
    updateCache() >> periodicCacheUpdate(interval)
  }

  private def periodicCacheUpdate(interval: FiniteDuration): F[Error Either Unit] = {
    val updateAndSchedule = updateCache() >> Timer[F].sleep(interval) >> periodicCacheUpdate(interval)
    Concurrent[F].start(updateAndSchedule).void.map(_.asRight[Error])
  }

}
