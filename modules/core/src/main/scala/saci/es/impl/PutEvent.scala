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

import saci.es.data._

trait PutEvent[F[_]] {

  def put(eventData: EventData): F[WriteResult]

}

object PutEvent {
  import saci.es.Repository
  import cats.MonadError
  import cats.implicits._
  import java.{util => ju}

  def apply[F[_]: MonadError[*[_], Throwable]: Repository]: PutEvent[F] =
    new PutEvent[F] {

      override def put(eventData: EventData): F[WriteResult] = {
        for {
          evId   <- resolveEventId(eventData)
          result <- Repository[F].put(evId, eventData.agType, eventData.agId, eventData.version, eventData.data)
        } yield result
      }

      private def resolveEventId(eventData: EventData) =
        eventData.evId match {
          case Some(id) => MonadError[F, Throwable].point(id)
          case None => MonadError[F, Throwable].catchNonFatal(ju.UUID.randomUUID())
        }

    }
}
