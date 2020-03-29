package saci.data

import io.circe.Json
import java.time.Instant

case class EventData(evId: Option[EventId], agType: AggregateType, agId: AggregateId, version: Version, data: Json)
case class RecordedEvent(evId: EventId, agType: AggregateType, agId: AggregateId, version: Version, data: Json, created: Instant)
case class WriteResult(eventId: EventId, agType: AggregateType, sqNr: SequenceNr)
