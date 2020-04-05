package saci.es

import saci.data._
import io.circe.Json

trait Repository[F[_]] {

  def query(sgType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, RecordedEvent]
  def put(evId: EventId, agType: AggregateType, agId: AggregateId, version: Version, data: Json): F[WriteResult]

}

object Repository {

  def apply[F[_]: Repository]: Repository[F] = implicitly
}
