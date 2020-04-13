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
