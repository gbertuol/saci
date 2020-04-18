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
import io.circe.syntax._
import java.{util => ju}

class GetAggregateSpec extends Specification with CatsEffect {

  "GetAggregate" should {
    "get the aggregate" in {
      for {
        repo <- InMemoryRepo.apply[IO]
        evId <- IO.delay(ju.UUID.randomUUID())
        agType = "agType"
        agId = "agId"
        version = 1L
        data = """{}""".asJson
        _ <- repo.createStream(agType)
        _ <- repo.put(evId, agType, agId, version, data)
      } yield {
        implicit val _repo = repo
        for {
          get <- GetAggregate.apply[IO].get(agType, agId, 1).compile.last
          _   <- IO(get.map(_.version) === Some(1))
          _   <- IO(get.flatMap(_.evId) === Some(evId))
          _   <- IO(get.map(_.data) === Some("""{}""".asJson))
        } yield success
      }
    }
  }
}
