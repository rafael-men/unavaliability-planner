create table password_tickets (
    id           bigint generated always as identity primary key,
    email        text        not null,
    user_id      bigint      references users5 (id) on delete set null,
    status       text        not null default 'open',  -- open | resolved
    resolved_by  bigint,
    resolved_at  timestamptz,
    created_at   timestamptz not null default now()
);

create index idx_password_tickets_status on password_tickets (status);
