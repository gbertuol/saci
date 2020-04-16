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
import saci.data._
// import io.circe.Json
import cats.effect._
import cats.implicits._
import skunk._
import skunk.implicits._
import skunk.codec.all._
// import skunk.circe.codec.all._
import natchez.Trace
import io.circe.Json
import scala.util.control.NonFatal

object PGRepository {

  def apply[F[_]: Concurrent: ContextShift: Trace]: Resource[F, Repository[F]] = {
    for {
      sessionPool <- buildPool
    } yield new Repository[F] {
      import Statements._

      override def query(agType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, RecordedEvent] = ???

      override def put(evId: EventId, agType: AggregateType, agId: AggregateId, version: Version, data: Json): F[WriteResult] = ???

      override def createStream(agType: AggregateType): F[Unit] =
        Trace[F].span(s"createStream $agType") {
          sessionPool.use { session =>
            for {
              _ <- session.prepare(insertNewStream).use(_.execute(agType)).recoverWith {
                case ex: skunk.exception.PostgresErrorException => Concurrent[F].raiseError(StreamAlreadyExistsError(ex.message))
                case NonFatal(ex) => Concurrent[F].raiseError(ex)
              }
            } yield ()
          }
        }

      override def listStreams: fs2.Stream[F, AggregateType] =
        for {
          session <- fs2.Stream.resource(sessionPool)
          ps      <- fs2.Stream.resource(session.prepare(selectStreams))
          streams <- ps.stream(Void, 10)
        } yield streams

    }

  }

  object Statements {
    val selectStreams: Query[Void, String] = sql"""select stream_name from eventstore.streams order by stream_name desc""".query(varchar(128))
    // val selectSpecificStream: Query[String, Int] = sql"""select stream_id from eventstore.streams where stream_name=$varchar(128)""".query(int4)
    val insertNewStream: Command[String] = sql"""insert into eventstore.streams (stream_name) values ($varchar)""".command
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
