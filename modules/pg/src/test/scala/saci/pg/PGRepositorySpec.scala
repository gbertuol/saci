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

import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import cats.effect.IO
import natchez.Trace
import scala.util.Random
import saci.data._
import io.circe.syntax._
import java.{util => ju}

class PGRepositorySpec extends Specification with CatsEffect {
  implicit val cs = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
  implicit val tracer = Trace.Implicits.noop[IO]

  "The PG repository" should {
    "create a stream" in {
      PGRepository.apply[IO].use { repo =>
        for {
          streamName <- newStreamName
          _          <- repo.createStream(streamName)
        } yield success
      }
    }
    "should not create a stream if it already exists (same name)" in {
      PGRepository.apply[IO].use { repo =>
        for {
          streamName <- newStreamName
          _          <- repo.createStream(streamName)
          attempted  <- repo.createStream(streamName).attempt
          _          <- IO { attempted must beLeft(_: StreamAlreadyExistsError) }
          streams    <- repo.listStreams.compile.toList
          _          <- IO { streams must contain(streamName) }
        } yield success
      }
    }
    "put a single event" in {
      PGRepository.apply[IO].use { repo =>
        for {
          streamName <- newStreamName
          _          <- repo.createStream(streamName)
          evId       <- IO { ju.UUID.randomUUID() }
          agId       <- IO { ju.UUID.randomUUID().toString }
          _          <- repo.put(evId, streamName, agId, 1L, """{"a": 1, "b": "val"}""".asJson)
          events     <- repo.query(streamName, agId, 1L).compile.toList
          _          <- IO { events must have size (1) }
          evId2      <- IO { ju.UUID.randomUUID() }
          _          <- repo.put(evId2, streamName, agId, 2L, """{"a": 2, "b": "val"}""".asJson)
          events2    <- repo.query(streamName, agId, 1L).compile.toList
          _          <- IO { events2 must have size (2) }
        } yield success
      }
    }
  }

  private def newStreamName: IO[String] = IO {
    s"stream-${Random.nextLong()}"
  }
}
