package saci.pg

import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import cats.effect.IO
// import cats.implicits._
import natchez.Trace
import scala.util.Random
import saci.data.StreamAlreadyExistsError

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
  }

  private def newStreamName: IO[String] = IO {
    s"stream-${Random.nextLong()}"
  }
}
