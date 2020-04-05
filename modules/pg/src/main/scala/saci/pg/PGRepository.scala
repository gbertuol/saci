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

// import saci.es.Repository
// import saci.data._
// import io.circe.Json
// import cats.effect._
// import cats.implicits._
// import skunk._
// import skunk.implicits._
// import skunk.circe.codec.all._
// import natchez.Trace

object PGRepository {

  // def apply[F[_]: Concurrent: ContextShift: Trace]: Resource[F, Repository[F]] = {
  //   for {
  //     p <- pool
  //   } yield new Repository[F] {
  //     override def query(aggregateId: AggregateId, from: SequenceNr): fs2.Stream[F, Json] = {
  //       for {
  //         s <- fs2.Stream.resource(p)
  //       } yield ()
  //     }

  //   }

  //   // null.asInstanceOf[Repository[F]]
  // }

  // val select: Query[String ~ Long, Json] =
  //   sql"""
  //       select
  //         event
  //       from
  //         eventstable
  //       where
  //         aggregateId = $varchar
  //         and sequenceNr >= $int8
  //     """.query(json)

  // private def pool[F[_]: Concurrent: ContextShift: Trace]: SessionPool[F] =
  //   Session.pooled(
  //     host = "localhost",
  //     user = "jimmy",
  //     database = "world",
  //     password = Some("banana"),
  //     max = 10
  //   )
}
