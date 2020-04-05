package saci.data

abstract class EventStoreError(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull)
abstract class RetriableError(message: String, cause: Option[Throwable]) extends EventStoreError(message, cause)
abstract class NonRetriableError(message: String, cause: Option[Throwable]) extends EventStoreError(message, cause)

final case class OptimisticConcurrencyCheckError(message: String) extends NonRetriableError(message, cause = None)
