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

package saci.es

import saci.data._

trait GetAggregate[F[_]] {

  def get(aggregateId: AggregateId, from: SequenceNr): fs2.Stream[F, String]
}

object GetAggregate {
  import cats.MonadError
  // import cats.syntax._
  // import io.circe._
  // import io.circe.generic.auto._
  // import io.circe.parser._
  // import io.circe.syntax._

  def apply[F[_]: Repository](implicit M: MonadError[F, Throwable]): GetAggregate[F] =
    new GetAggregate[F] {
      override def get(aggregateId: AggregateId, from: SequenceNr): fs2.Stream[F, String] = {
        for {
          jsonPayload    <- Repository[F].query(aggregateId, from)
          decodedPayload <- fs2.Stream.fromEither[F](jsonPayload.as[String])
        } yield decodedPayload
      }
    }
}
