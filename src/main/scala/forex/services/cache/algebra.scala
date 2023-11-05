package forex.services.cache

import forex.domain.Rate
import forex.services.rates.errors._

trait Algebra[F[_]] {
  def get(pairs: List[Rate.Pair]): F[Error Either List[Rate]]
  def init(): F[Error Either Unit]
}
