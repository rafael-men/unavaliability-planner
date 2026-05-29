CREATE TABLE IF NOT EXISTS users5 (
  id          BIGSERIAL PRIMARY KEY,
  email       TEXT UNIQUE NOT NULL,
  passw       TEXT NOT NULL,
  nome        TEXT NOT NULL,
  role        TEXT NOT NULL DEFAULT 'colaborador'
              CHECK (role IN ('admin_master','admin_editor','admin_leitor','socio','lider','colaborador')),
  status      TEXT NOT NULL DEFAULT 'pending'
              CHECK (status IN ('pending','approved','rejected')),
  department  TEXT,
  member_id   BIGINT,
  approved_by BIGINT,
  approved_at TIMESTAMPTZ,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users5_email      ON users5(email);
CREATE INDEX IF NOT EXISTS idx_users5_status     ON users5(status);
CREATE INDEX IF NOT EXISTS idx_users5_role       ON users5(role);
CREATE INDEX IF NOT EXISTS idx_users5_department ON users5(department);


CREATE TABLE IF NOT EXISTS members (
  id              BIGSERIAL PRIMARY KEY,
  name            TEXT NOT NULL,
  email           TEXT UNIQUE,
  area            TEXT,
  squad           TEXT,
  funcao          TEXT,
  report_to       TEXT,
  operacoes       BOOLEAN DEFAULT FALSE,
  day_offs_quota  INTEGER DEFAULT 20,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_members_email     ON members(email);
CREATE INDEX IF NOT EXISTS idx_members_area      ON members(area);
CREATE INDEX IF NOT EXISTS idx_members_report_to ON members(report_to);


CREATE TABLE IF NOT EXISTS unavailability (
  id                   BIGSERIAL PRIMARY KEY,
  user_id              BIGINT NOT NULL REFERENCES users5(id) ON DELETE CASCADE,
  full_name            TEXT NOT NULL,
  unavailability_type  TEXT NOT NULL CHECK (unavailability_type IN ('prolongado','pontual')),
  department           TEXT NOT NULL,
  start_date           DATE NOT NULL,
  end_date             DATE NOT NULL,
  total_days           INTEGER NOT NULL,
  status               TEXT NOT NULL DEFAULT 'pending'
                       CHECK (status IN ('pending','approved','rejected')),
  reviewed_by          BIGINT,
  reviewed_at          TIMESTAMPTZ,
  created_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_unavail_user_id    ON unavailability(user_id);
CREATE INDEX IF NOT EXISTS idx_unavail_status     ON unavailability(status);
CREATE INDEX IF NOT EXISTS idx_unavail_department ON unavailability(department);
CREATE INDEX IF NOT EXISTS idx_unavail_dates      ON unavailability(start_date, end_date);


ALTER TABLE users5
  DROP CONSTRAINT IF EXISTS fk_users5_member;
ALTER TABLE users5
  ADD CONSTRAINT fk_users5_member
  FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL;


CREATE TABLE IF NOT EXISTS eventos (
  id           BIGSERIAL PRIMARY KEY,
  nome         TEXT NOT NULL,
  descricao    TEXT,
  data_inicio  DATE NOT NULL,
  data_fim     DATE NOT NULL,
  cliente_a    BOOLEAN NOT NULL DEFAULT FALSE,
  cliente_b    BOOLEAN NOT NULL DEFAULT FALSE,
  cliente_c    BOOLEAN NOT NULL DEFAULT FALSE,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  CONSTRAINT chk_eventos_datas CHECK (data_fim >= data_inicio)
);

CREATE INDEX IF NOT EXISTS idx_eventos_dates     ON eventos(data_inicio, data_fim);
CREATE INDEX IF NOT EXISTS idx_eventos_cliente_a ON eventos(cliente_a) WHERE cliente_a = TRUE;
CREATE INDEX IF NOT EXISTS idx_eventos_cliente_b ON eventos(cliente_b) WHERE cliente_b = TRUE;
CREATE INDEX IF NOT EXISTS idx_eventos_cliente_c ON eventos(cliente_c) WHERE cliente_c = TRUE;


CREATE TABLE IF NOT EXISTS clientes (
  id          BIGSERIAL PRIMARY KEY,
  nome        TEXT NOT NULL UNIQUE,
  descricao   TEXT,
  ativo       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clientes_ativo ON clientes(ativo) WHERE ativo = TRUE;


CREATE TABLE IF NOT EXISTS user_clientes (
  user_id     BIGINT NOT NULL REFERENCES users5(id)   ON DELETE CASCADE,
  cliente_id  BIGINT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
  ativo       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, cliente_id)
);

CREATE INDEX IF NOT EXISTS idx_user_clientes_user    ON user_clientes(user_id)    WHERE ativo = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_clientes_cliente ON user_clientes(cliente_id) WHERE ativo = TRUE;


CREATE TABLE IF NOT EXISTS evento_clientes (
  evento_id   BIGINT NOT NULL REFERENCES eventos(id)  ON DELETE CASCADE,
  cliente_id  BIGINT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (evento_id, cliente_id)
);

CREATE INDEX IF NOT EXISTS idx_evento_clientes_evento  ON evento_clientes(evento_id);
CREATE INDEX IF NOT EXISTS idx_evento_clientes_cliente ON evento_clientes(cliente_id);
