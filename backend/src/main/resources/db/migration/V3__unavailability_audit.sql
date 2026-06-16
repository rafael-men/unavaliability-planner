create table unavailability_audit (
    id                bigint generated always as identity primary key,
    unavailability_id bigint      not null references unavailability (id) on delete cascade,
    action            text        not null,
    actor_id          bigint,
    actor_name        text,
    detail            text,
    created_at        timestamptz not null default now()
);

create index idx_unav_audit_unav_id on unavailability_audit (unavailability_id);
