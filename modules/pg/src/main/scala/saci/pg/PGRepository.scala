package saci.pg

import saci.es.Repository
import saci.data._
import io.circe.Json
import cats.effect._
import cats.implicits._
import skunk._
import skunk.implicits._
import skunk.circe.codec.all._
import natchez.Trace

object PGRepository {

  def apply[F[_]: Concurrent: ContextShift: Trace]: Resource[F, Repository[F]] = {
    for {
      p <- pool
    } yield new Repository[F] {
      override def query(aggregateId: AggregateId, from: SequenceNr): fs2.Stream[F, Json] = {
        for {
          s <- fs2.Stream.resource(p)
        } yield ()
      }

    }

    // null.asInstanceOf[Repository[F]]
  }

  val select: Query[String ~ Long, Json] =
    sql"""
        select
          event
        from
          eventstable
        where
          aggregateId = $varchar
          and sequenceNr >= $int8
      """.query(json)

  private def pool[F[_]: Concurrent: ContextShift: Trace]: SessionPool[F] =
    Session.pooled(
      host = "localhost",
      user = "jimmy",
      database = "world",
      password = Some("banana"),
      max = 10
    )
}
