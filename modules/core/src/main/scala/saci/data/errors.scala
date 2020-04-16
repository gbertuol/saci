// Copyright (c) 2019 Guilherme Bertuol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package saci.data

abstract class EventStoreError(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull)
abstract class RetriableError(message: String, cause: Option[Throwable]) extends EventStoreError(message, cause)
abstract class NonRetriableError(message: String, cause: Option[Throwable]) extends EventStoreError(message, cause)

final case class OptimisticConcurrencyCheckError(message: String) extends NonRetriableError(message, cause = None)
final case class StreamNotFoundError(message: String) extends NonRetriableError(message, cause = None)
final case class StreamAlreadyExistsError(message: String) extends NonRetriableError(message, cause = None)
final case class FatalRepositoryError(message: String, cause: Throwable) extends NonRetriableError(message, cause = Some(cause))
