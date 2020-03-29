package saci.es

import saci.data._
import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import cats.effect.IO
// import cats.implicits._
import io.circe.Json
import io.circe.syntax._

class GetAggregateSpec extends Specification with CatsEffect {

  implicit val repo = new Repository[IO] {
    override def query(aggregateId: AggregateId, from: SequenceNr): fs2.Stream[IO, Json] = {
      fs2.Stream.emit(
        """{"a": 1}""".asJson
      )
    }
  }

  "GetAggregate" should {
    "get the aggregate" in {
      for {
        get <- GetAggregate.apply[IO].get("foo", 1L).compile.last
        _   <- IO(get === Some("""{"a": 1}"""))
      } yield success
    }
  }
}
