package saci.es.impl

import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import cats.effect.IO
import saci.data.EventData
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
