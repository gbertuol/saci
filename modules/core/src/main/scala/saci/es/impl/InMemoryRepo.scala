// Copyright (c) 2019 Guilherme Bertuol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package saci.es.impl

import java.time.Instant

object InMemoryRepo {
  import saci.data._
  import saci.es.Repository
  import scala.collection.mutable
  import io.circe.Json
  import saci.data.WriteResult
  import cats.effect.Sync
  import cats.effect.concurrent.Ref
  import cats.implicits._

  def apply[F[_]: Sync]: F[Repository[F]] = {
    for {
      db            <- Ref[F].of(mutable.Map.empty[AggregateType, mutable.Map[AggregateId, mutable.Buffer[Node]]])
      globalCounter <- Ref[F].of(0L)
    } yield new Repository[F] {
      override def query(agType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, RecordedEvent] = {
        for {
          _db <- fs2.Stream.eval(db.get)
          _ <- if (_db.contains(agType)) fs2.Stream(_db(agType))
          else fs2.Stream.raiseError[F](StreamNotFoundError(s"unable to find stream type $agType"))
          stream = _db(agType)
          aggregate = stream.getOrElse(agId, mutable.Buffer.empty[Node])
          data = aggregate.dropWhile(_.version < from).map(d => RecordedEvent(d.evId, agType, agId, d.version, d.data, d.created))
          events <- fs2.Stream.fromIterator(data.iterator)
        } yield events
      }

      override def put(evId: EventId, agType: AggregateType, agId: AggregateId, version: Version, data: Json): F[WriteResult] = {
        for {
          _db       <- db.get
          timestamp <- Sync[F].delay(Instant.now)
          _ <- Sync[F].catchNonFatal {
            val stream = if (_db.contains(agType)) _db(agType) else throw StreamNotFoundError(s"unable to find stream type $agType")
            val aggregate = stream.getOrElseUpdate(agId, mutable.Buffer.empty[Node])
            if (aggregate.isEmpty) {
              if (version != 1) {
                throw OptimisticConcurrencyCheckError(s"empty stream of $agId requires first version to be 1, got $version instead")
              }
              aggregate.addOne(Node(version, evId, timestamp, data))
            } else {
              val lastNode = aggregate.last
              if (lastNode.version != (version - 1)) {
                throw OptimisticConcurrencyCheckError(s"outdated version $version of $agId current is ${lastNode.version}")
              }
              aggregate.addOne(Node(version, evId, timestamp, data))
            }
          }
          sqNr <- globalCounter.modify(x => (x + 1, x))
        } yield WriteResult(evId, agType, sqNr)
      }

      override def listStreams: fs2.Stream[F, AggregateType] = {
        for {
          _db     <- fs2.Stream.eval(db.get)
          streams <- fs2.Stream.fromIterator(_db.keys.iterator)
        } yield streams
      }

      override def createStream(agType: AggregateType): F[Unit] = {
        for {
          _db <- db.get
          _ <- Sync[F].catchNonFatal {
            if (_db.contains(agType)) throw StreamAlreadyExistsError(s"unable to re-create existing stream $agType")
            _db.addOne(agType -> mutable.Map.empty)
          }
        } yield ()
      }
    }
  }

  final case class Node(version: Version, evId: EventId, created: Instant, data: Json)
}
