package saci.es.impl

import java.time.Instant

object InMemoryRepo {
  import saci.data._
  import scala.collection.mutable
  import io.circe.Json
  import saci.data.WriteResult
  import cats.effect.Sync
  import cats.effect.concurrent.Ref
  import cats.implicits._

  def apply[F[_]: Sync]: F[Repository[F]] = {
    for {
      db            <- Ref[F].of(mutable.Map.empty[Key, mutable.Buffer[Node]])
      globalCounter <- Ref[F].of(0L)
    } yield new Repository[F] {
      override def query(agType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, RecordedEvent] = {
        for {
          _db <- fs2.Stream.eval(db.get)
          aggregate = _db.getOrElse(Key(agType, agId), mutable.Buffer.empty[Node])
          data = aggregate.dropWhile(_.version < from).map(d => RecordedEvent(d.evId, agType, agId, d.version, d.data, d.created))
          events <- fs2.Stream.fromIterator(data.iterator)
        } yield events
      }

      override def put(evId: EventId, agType: AggregateType, agId: AggregateId, version: Version, data: Json): F[WriteResult] = {
        for {
          _db       <- db.get
          timestamp <- Sync[F].delay(Instant.now)
          _ <- Sync[F].catchNonFatal {
            val key = Key(agType, agId)
            val aggregate = _db.getOrElseUpdate(key, mutable.Buffer.empty[Node])
            if (aggregate.isEmpty) {
              if (version != 1) {
                throw OptimisticConcurrencyCheckError(s"empty stream of $key requires first version to be 1, got $version instead")
              }
              aggregate.addOne(Node(version, evId, timestamp, data))
            } else {
              val lastNode = aggregate.last
              if (lastNode.version != (version - 1)) {
                throw OptimisticConcurrencyCheckError(s"outdated version $version of $key current is ${lastNode.version}")
              }
              aggregate.addOne(Node(version, evId, timestamp, data))
            }
          }
          sqNr <- globalCounter.modify(x => (x + 1, x))
        } yield WriteResult(evId, agType, sqNr)
      }
    }
  }

  final case class Key(agType: AggregateType, agId: AggregateId)
  final case class Node(version: Version, evId: EventId, created: Instant, data: Json)
}
