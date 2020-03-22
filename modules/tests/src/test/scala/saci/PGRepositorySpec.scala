package saci.pg

import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import cats.effect.IO

class PGRepositorySpec extends Specification with CatsEffect {

  "This first test" should {
    "run an effectful test" in IO {
      true must beFalse
    }
  }
}
