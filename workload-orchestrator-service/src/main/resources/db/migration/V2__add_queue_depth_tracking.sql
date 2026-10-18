-- Adds queue-depth sample tracking, the missing input for scale-UP decisions
-- (V1 only supported idle-timeout scale-DOWN — see IdleAutoStopScheduler).

create table queue_depth_sample (
    id                 uuid primary key,
    engine_id          uuid        not null references engine (id) on delete cascade,
    queued_query_count int         not null,
    running_query_count int        not null,
    sampled_at         timestamptz not null default now()
);
-- The scale-up evaluator's core query: "recent samples for this engine, newest
-- first" — a rolling window, not a full history scan.
create index idx_queue_depth_sample_engine_time on queue_depth_sample (engine_id, sampled_at desc);

-- Samples older than a short retention window are pruned by the scale-up
-- scheduler itself (see ScaleUpScheduler) rather than needing a separate cleanup
-- job — this table is meant to hold minutes of history, not a permanent log
-- (that's what engine_scaling_event is for).
