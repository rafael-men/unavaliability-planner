create index if not exists idx_unavailability_analytics_period
    on unavailability (status, start_date, end_date);

create index if not exists idx_unavailability_created_status
    on unavailability (created_at, status);

create index if not exists idx_user_clientes_active_user_client
    on user_clientes (user_id, cliente_id)
    where ativo = true;

create index if not exists idx_evento_clientes_client_event
    on evento_clientes (cliente_id, evento_id);

create index if not exists idx_eventos_period
    on eventos (data_inicio, data_fim);