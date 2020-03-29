package saci.es

import saci.data._

trait Repository[F[_]] {

  def query(sgType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, RecordedEvent]
}

object Repository {

  def apply[F[_]: Repository]: Repository[F] = implicitly
}
