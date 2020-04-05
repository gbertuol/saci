package saci.es

import saci.data.EventData
import saci.data.WriteResult

trait PutEvent[F[_]] {

  def put(eventData: EventData): F[WriteResult]

}

object PutEvent {
  import cats.MonadError
  import cats.implicits._
  import java.{util => ju}

  def apply[F[_]: MonadError[*[_], Throwable]: Repository]: PutEvent[F] =
    new PutEvent[F] {

      override def put(eventData: EventData): F[WriteResult] = {
        for {
          evId   <- resolveEventId(eventData)
          result <- Repository[F].put(evId, eventData.agType, eventData.agId, eventData.version, eventData.data)
        } yield result
      }

      private def resolveEventId(eventData: EventData) =
        eventData.evId match {
          case Some(id) => MonadError[F, Throwable].point(id)
          case None => MonadError[F, Throwable].catchNonFatal(ju.UUID.randomUUID())
        }

    }
}
