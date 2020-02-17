
CREATE SCHEMA IF NOT EXISTS eventstore;

CREATE TABLE IF NOT EXISTS eventstore.events(
  global_position BIGSERIAL,
  created_utc TIMESTAMP NOT NULL,
  aggregate_id VARCHAR(128) NOT NULL,
  sequence_nr BIGINT NOT NULL,
  payload JSONB NOT NULL,
  metadata JSONB,
  CONSTRAINT pk_events PRIMARY KEY(global_position),
  CONSTRAINT uq_aggregate_id_and_sequence_nr UNIQUE(aggregate_id, sequence_nr),
  CONSTRAINT ck_sequence_nr_gte_zero CHECK (sequence_nr >= 0)
);

-- CREATE INDEX IF NOT EXISTS ix_aggregate_id 
--   ON eventstore.events (aggregate_id, sequence_nr);
