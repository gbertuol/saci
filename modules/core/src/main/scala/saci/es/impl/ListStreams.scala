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

trait ListStreams[F[_]] {
  def listStreams: fs2.Stream[F, AggregateType]
  def listEvents(agType: AggregateType, from: Option[SequenceNr]): fs2.Stream[F, EventData]
}

object ListStreams {
  import saci.es.Repository
  import cats.implicits._

  def apply[F[_]: Repository]: ListStreams[F] =
    new ListStreams[F] {
      override def listStreams: fs2.Stream[F, AggregateType] =
        Repository[F].listStreams

      override def listEvents(agType: AggregateType, from: Option[SequenceNr]): fs2.Stream[F, EventData] =
        Repository[F]
          .listEvents(agType, from)
          .map(ev => EventData(ev.evId.some, ev.agType, ev.agId, ev.version, ev.data))
    }
}
