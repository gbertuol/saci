package saci.es

import saci.data._
import io.circe.Json

trait Repository[F[_]] {

  def query(aggregateId: AggregateId, from: SequenceNr): fs2.Stream[F, Json]
}

object Repository {

  def apply[F[_]: Repository]: Repository[F] = implicitly
}
