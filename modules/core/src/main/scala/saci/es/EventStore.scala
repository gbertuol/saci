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

trait EventStore[F[_]] {

  // Events
  def get(agType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, EventData]
  def put(eventData: EventData): F[WriteResult]
  def list(agType: AggregateType, agId: AggregateId): fs2.Stream[F, EventData]

  // Streams
  def list(agType: AggregateType, from: Option[SequenceNr]): fs2.Stream[F, EventData]
  def create(agType: AggregateType): F[Unit]
}

object EventStore {
  import saci.es.impl._
  import cats.effect.Sync

  def apply[F[_]: Sync: Repository]: EventStore[F] =
    new EventStore[F] {
      override def get(agType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[F, EventData] =
        GetAggregate.apply[F].get(agType, agId, from)

      override def put(eventData: EventData): F[WriteResult] =
        PutEvent.apply[F].put(eventData)

      override def list(agType: AggregateType, agId: AggregateId): fs2.Stream[F, EventData] =
        GetAggregate.apply[F].get(agType, agId, from = 0)

      override def list(agType: AggregateType, from: Option[SequenceNr]): fs2.Stream[F, EventData] = ???
      override def create(agType: AggregateType): F[Unit] = ???
    }
}
