package saci.es

import saci.data._
import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import cats.effect.IO
import io.circe.syntax._
import java.{util => ju}
import java.time.Instant

class GetAggregateSpec extends Specification with CatsEffect {

  implicit val repo = new Repository[IO] {
    override def query(sgType: AggregateType, agId: AggregateId, from: Version): fs2.Stream[IO, RecordedEvent] = {
      fs2.Stream.emit(
        RecordedEvent(
          ju.UUID.randomUUID(),
          sgType,
          agId,
          from,
          """{"a": 1}""".asJson,
          Instant.MIN
        )
      )
    }
  }

  "GetAggregate" should {
    "get the aggregate" in {
      for {
        get <- GetAggregate.apply[IO].get("foo", "bar", 1).compile.last
        _   <- IO(get.map(_.version) === Some(1))
        _   <- IO(get.map(_.data) === Some("""{"a": 1}""".asJson))
      } yield success
    }
  }
}
