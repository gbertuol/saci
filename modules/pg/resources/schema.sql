CREATE SCHEMA IF NOT EXISTS eventstore;
CREATE TABLE IF NOT EXISTS eventstore.streams (
  stream_id bigserial,
  stream_name varchar(128) NOT NULL,
  sequence_nr bigint NOT NULL DEFAULT (0),
  metadata jsonb,
  CONSTRAINT pk_streams PRIMARY KEY (stream_id),
  CONSTRAINT uq_stream_name UNIQUE (stream_name),
  CONSTRAINT ck_sequence_nr_gte_zero CHECK (sequence_nr >= 0)
);
CREATE INDEX IF NOT EXISTS ix_stream_name_reversed ON eventstore.streams (REVERSE(stream_name));
CREATE TABLE IF NOT EXISTS eventstore.events (
  global_position bigserial,
  created_utc timestamp NOT NULL,
  event_id uuid NOT NULL,
  stream_id bigint NOT NULL,
  aggregate_id varchar(128) NOT NULL,
  aggregate_version bigint NOT NULL,
  payload jsonb NOT NULL,
  metadata jsonb,
  CONSTRAINT pk_events PRIMARY KEY (global_position),
  CONSTRAINT fk_events_streams FOREIGN KEY (stream_id) REFERENCES eventstore.streams (stream_id),
  CONSTRAINT uq_stream_id_and_aggregate_id_and_aggregate_version UNIQUE (stream_id, aggregate_id, aggregate_version),
  CONSTRAINT uq_event_id UNIQUE (event_id)
);
CREATE INDEX IF NOT EXISTS ix_aggregate_id ON eventstore.events (aggregate_id, aggregate_version);