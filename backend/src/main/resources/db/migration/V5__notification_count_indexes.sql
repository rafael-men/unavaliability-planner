create index if not exists idx_users5_status
    on users5 (status);

create index if not exists idx_unavailability_status_user_id
    on unavailability (status, user_id);