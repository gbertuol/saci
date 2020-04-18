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

class ListStreamsSpec extends Specification with CatsEffect {

  "ListStreams" should {
    "list empty if no streams" in {
      for {
        repo <- InMemoryRepo.apply[IO]
      } yield {
        implicit val _repo = repo
        for {
          result <- ListStreams.apply[IO].listStreams.compile.toList
          _      <- IO(result.size === 0)
        } yield success
      }
    }
    "list created streams" in {
      for {
        repo <- InMemoryRepo.apply[IO]
        _    <- repo.createStream("agType")
      } yield {
        implicit val _repo = repo
        for {
          result <- ListStreams.apply[IO].listStreams.compile.toList
          _      <- IO(result === List("agType"))
        } yield success
      }
    }
  }
}
