package saci

import cats.effect.concurrent.Ref
import cats.Monad
import cats.syntax.all._

package object utils {

  implicit final class RefOps[F[_], A](private val ref: Ref[F, A]) extends AnyVal {

    def modifyOr[E, B](f: A => Either[E, (A, B)])(implicit F: Monad[F]): F[Either[E, B]] =
      ref.access.flatMap {
        case (a, setter) =>
          f(a) match {
            case Right((a, b)) => setter(a).ifM(ifTrue = F.pure(Right(b)), ifFalse = modifyOr(f))
            case l @ Left(_) => F.pure(l.rightCast[B])
          }
      }

    def updateOr[E](f: A => Either[E, A])(implicit F: Monad[F]): F[Option[E]] =
      modifyOr(a => f(a).map(_ -> ())).map(_.swap.toOption)
  }

}
