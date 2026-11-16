-- Índices compostos estratégicos para melhorar o desempenho das consultas

CREATE INDEX IF NOT EXISTS idx_unav_status_dates
    ON unavailability (status, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_unav_user_status
    ON unavailability (user_id, status);

CREATE INDEX IF NOT EXISTS idx_unav_created_at_desc
    ON unavailability (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_members_report_to
    ON members (report_to);

CREATE INDEX IF NOT EXISTS idx_eventos_dates
    ON eventos (data_inicio, data_fim);
