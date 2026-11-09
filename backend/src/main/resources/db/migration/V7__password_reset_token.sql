alter table password_tickets
    add column token_hash text;

alter table password_tickets
    add column token_expires_at timestamptz;

alter table password_tickets
    add column token_used_at timestamptz;

create unique index idx_password_tickets_token_hash
    on password_tickets (token_hash)
    where token_hash is not null;