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

import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import cats.effect.IO
import saci.es.data.EventData
import io.circe.syntax._
import java.{util => ju}

class PutEventSpec extends Specification with CatsEffect {

  "PutEvent" should {
    "generate an event id and store the event" in {
      for {
        repo <- InMemoryRepo.apply[IO]
        _    <- repo.createStream("agType")
      } yield {
        implicit val _repo = repo
        val eventData = EventData(evId = None, "agType", "agId", version = 1, data = "{}".asJson)
        for {
          writeResult <- PutEvent.apply[IO].put(eventData)
          _           <- IO(writeResult.agType === "agType")
          get         <- GetAggregate.apply[IO].get("agType", "agId", from = 1).compile.last
          _           <- IO(get.flatMap(_.evId) === Some(writeResult.eventId))
        } yield success
      }
    }
    "use the given event id and store the event" in {
      for {
        repo <- InMemoryRepo.apply[IO]
        evId <- IO(ju.UUID.randomUUID())
        _    <- repo.createStream("agType")
      } yield {
        implicit val _repo = repo
        val eventData = EventData(evId = Some(evId), "agType", "agId", version = 1, data = "{}".asJson)
        for {
          writeResult <- PutEvent.apply[IO].put(eventData)
          _           <- IO(writeResult.eventId === evId)
          get         <- GetAggregate.apply[IO].get("agType", "agId", from = 1).compile.last
          _           <- IO(get.flatMap(_.evId) === Some(writeResult.eventId))
        } yield success
      }
    }
  }

}
