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

object InMemoryRepo {
  import saci.es.data._
  import saci.es.Repository
  import saci.utils._
  import scala.collection.mutable
  import io.circe.Json
  import cats.effect.Sync
  import cats.effect.concurrent.Ref
  import cats.implicits._
  import java.time.Instant

  final case class State(
      events: mutable.Map[AggregateType, mutable.Map[AggregateId, mutable.ArrayBuffer[Node]]] = mutable.Map(),
      globalCounter: mutable.ArrayBuffer[(AggregateType, AggregateId, Version)] = mutable.ArrayBuffer()
  )

  final case class Node(version: Version, evId: EventId, created: Instant, data: Json)

  def apply[F[_]: Sync]: F[Repository[F]] = {
    for {
      state <- Ref[F].of(State())
    } yield new Repository[F] {
      override def query(agType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, RecordedEvent] = {
        for {
          _db    <- fs2.Stream.eval(state.get)
          stream <- fs2.Stream.fromEither[F](_db.events.get(agType).toRight(StreamNotFoundError(s"unable to find stream type $agType")))
          aggregate = stream.getOrElse(agId, mutable.Buffer.empty[Node])
          node <- fs2.Stream.fromIterator(aggregate.iterator).dropWhile(_.version < from)
        } yield RecordedEvent(node.evId, agType, agId, node.version, node.data, node.created)
      }

      override def put(evId: EventId, agType: AggregateType, agId: AggregateId, version: Version, data: Json): F[WriteResult] = {
        for {
          timestamp <- Sync[F].delay(Instant.now)
          result <- state.modifyOr { _state =>
            for {
              stream <- _state.events.get(agType).toRight(StreamNotFoundError(s"unable to find stream type $agType"))
              aggregate = stream.getOrElseUpdate(agId, mutable.ArrayBuffer.empty[Node])
              gc <- {
                if (aggregate.isEmpty) {
                  if (version != 1) {
                    Left(OptimisticConcurrencyCheckError(s"empty stream of $agId requires first version to be 1, got $version instead"))
                  } else {
                    aggregate.addOne(Node(version, evId, timestamp, data))
                    _state.globalCounter.addOne((agType, agId, version))
                    Right(_state.globalCounter.size)
                  }
                } else {
                  val lastNode = aggregate.last
                  if (lastNode.version != (version - 1)) {
                    Left(OptimisticConcurrencyCheckError(s"outdated version $version of $agId current is ${lastNode.version}"))
                  }
                  aggregate.addOne(Node(version, evId, timestamp, data))
                  _state.globalCounter.addOne((agType, agId, version))
                  Right(_state.globalCounter.size)
                }
              }
            } yield _state -> WriteResult(evId, agType, gc.toLong)
          }
          writeResult <- result.liftTo[F]
        } yield writeResult
      }

      override def listEvents(agType: AggregateType, from: Option[SequenceNr]): fs2.Stream[F, RecordedEvent] =
        for {
          _state <- fs2.Stream.eval(state.get)
          _from = from.getOrElse(0L)
          _until = _state.globalCounter.size
          data = _state.globalCounter.slice(_from.toInt, _until).collect {
            case (`agType`, agId, version) =>
              val node = _state.events(agType)(agId)(version.toInt - 1)
              RecordedEvent(node.evId, agType, agId, version, node.data, node.created)
          }
          stream <- fs2.Stream.fromIterator(data.iterator)
        } yield stream

      override def listStreams: fs2.Stream[F, AggregateType] =
        for {
          _state  <- fs2.Stream.eval(state.get)
          streams <- fs2.Stream.fromIterator(_state.events.keys.iterator)
        } yield streams

      override def createStream(agType: AggregateType): F[Unit] = {
        state
          .updateOr {
            case _state =>
              if (_state.events.contains(agType)) {
                Left(StreamAlreadyExistsError(s"unable to re-create existing stream $agType"))
              } else {
                _state.events.addOne(agType -> mutable.Map.empty)
                Right(_state)
              }
          }
          .flatMap {
            case Some(error) => Sync[F].raiseError(error)
            case _ => Sync[F].pure(())
          }
      }
    }
  }
}
