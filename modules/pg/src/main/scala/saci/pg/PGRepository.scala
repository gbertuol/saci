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

package saci.pg

import saci.es.Repository
import saci.es.data._
import cats.effect._
import cats.implicits._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.all._
import natchez.Trace
import io.circe.Json
import java.time.LocalDateTime
import java.time.ZoneOffset

object PGRepository {

  def apply[F[_]: Concurrent: ContextShift: Trace]: Resource[F, Repository[F]] = {
    for {
      sessionPool <- buildPool
    } yield new Repository[F] {
      import Statements._

      override def query(agType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, RecordedEvent] = {
        val events = for {
          session <- fs2.Stream.resource(sessionPool)
          ps      <- fs2.Stream.resource(session.prepare(selectEvents))
          ev      <- ps.stream(agId ~ from, 10)
        } yield RecordedEvent(ev.evId, agType, agId, ev.version, ev.data, ev.timestamp.toInstant(ZoneOffset.UTC))
        events.adaptError {
          case ex: skunk.exception.PostgresErrorException => println(ex); FatalRepositoryError("unexpected error", ex)
        }
      }

      override def put(evId: EventId, agType: AggregateType, agId: AggregateId, version: Version, data: Json): F[WriteResult] = {
        Trace[F].span(s"put $evId $agType $version") {
          sessionPool.use { session =>
            session.transaction
              .use { _ =>
                for {
                  streamId <- session.prepare(selectStream).use(_.unique(agType)).adaptError {
                    case _: skunk.exception.SkunkException => StreamNotFoundError(s"Unable to find aggregate type $agType")
                  }
                  sequenceNr <- session.prepare(insertNewEvent).use(_.unique(NewEvent(evId, streamId, agId, version, data))).adaptError {
                    case _: skunk.exception.SkunkException => OptimisticConcurrencyCheckError("out dated attempt")
                  }
                  _ <- session.prepare(updateSequenceNr).use(_.execute(sequenceNr ~ streamId))
                } yield WriteResult(evId, agType, sequenceNr)
              }
              .adaptError {
                case ex: skunk.exception.SkunkException => FatalRepositoryError("unexpected error", ex)
              }
          }
        }
      }

      override def createStream(agType: AggregateType): F[Unit] =
        Trace[F].span(s"createStream $agType") {
          sessionPool.use { session =>
            session.prepare(insertNewStream).use(_.execute(agType)).adaptError {
              case ex: skunk.exception.PostgresErrorException if ex.code == "23505" => StreamAlreadyExistsError(ex.message)
              case ex: skunk.exception.SkunkException => FatalRepositoryError("unexpected error", ex)
            } *> Concurrent[F].unit
          }
        }

      override def listStreams: fs2.Stream[F, AggregateType] = {
        val streams = for {
          session <- fs2.Stream.resource(sessionPool)
          ps      <- fs2.Stream.resource(session.prepare(selectStreams))
          streams <- ps.stream(Void, 10)
        } yield streams
        streams.adaptError {
          case ex: skunk.exception.SkunkException => FatalRepositoryError("unexpected error", ex)
        }
      }

    }

  }

  object Statements {
    val selectStreams: Query[Void, AggregateType] =
      sql"""select stream_name from eventstore.streams order by stream_name desc""".query(varchar(128))

    val selectStream: Query[String, StreamId] =
      sql"""select stream_id from eventstore.streams where stream_name=${varchar(128)}""".query(int8)

    val insertNewStream: Command[AggregateType] =
      sql"""insert into eventstore.streams (stream_name) values ($varchar)""".command

    final case class NewEvent(evId: EventId, streamId: StreamId, agId: AggregateId, version: Version, data: Json)
    private val newEvent: Encoder[NewEvent] = (uuid ~ int8 ~ varchar(128) ~ int8 ~ jsonb).gcontramap[NewEvent]

    val insertNewEvent: Query[NewEvent, SequenceNr] =
      sql"""insert into eventstore.events (created_utc, event_id, stream_id, aggregate_id, aggregate_version, payload)
          values ('now', $newEvent)
          returning global_position
         """
        .query(int8)

    val updateSequenceNr: Command[SequenceNr ~ StreamId] =
      sql"""update eventstore.streams set sequence_nr = $int8 where stream_id = $int8""".command

    final case class SelectedEvent(evId: EventId, version: Version, data: Json, timestamp: LocalDateTime)

    val selectEvents =
      sql"""select event_id, aggregate_version, payload, created_utc from eventstore.events
            where aggregate_id = ${varchar(128)} and aggregate_version >= $int8
         """
        .query(uuid ~ int8 ~ jsonb ~ timestamp)
        .gmap[SelectedEvent]

  }

  private def buildPool[F[_]: Concurrent: ContextShift: Trace]: SessionPool[F] =
    Session.pooled(
      host = "localhost",
      user = "mryoung",
      database = "eventstore",
      password = Some("whoscalling"),
      max = 10
      // debug = true
    )
}
