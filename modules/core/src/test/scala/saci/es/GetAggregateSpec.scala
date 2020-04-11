package saci.es

import saci.es.impl._
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
        version = 1
        data = """{}""".asJson
        _ <- repo.put(evId, agType, agId, version, data)
      } yield {
        implicit val _repo = repo
        for {
          get <- GetAggregate.apply[IO].get(agType, agId, 1).compile.last
          _   <- IO(get.map(_.version) === Some(1))
          _   <- IO(get.map(_.data) === Some("""{}""".asJson))
        } yield success
      }
    }
  }
}
