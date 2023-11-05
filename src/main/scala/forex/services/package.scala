package forex

package object services {
  type RatesClient[F[_]] = rates.Algebra[F]
  final val RatesClient = rates.Interpreters

  type RatesService[F[_]] = cache.Algebra[F]
  final val RatesServices = cache.Interpreters
}
