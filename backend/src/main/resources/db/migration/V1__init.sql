create table members (
    id             bigint generated always as identity primary key,
    name           text        not null,
    email          text        not null unique,
    area           text,
    squad          text,
    funcao         text,
    report_to      text,
    operacoes      boolean,
    day_offs_quota integer,
    created_at     timestamptz not null default now()
);

create table users5 (
    id          bigint generated always as identity primary key,
    email       text        not null unique,
    nome        text,
    passw       text,
    role        text,        
    status      text,       
    department  text,
    member_id   bigint references members (id) on delete set null,
    approved_by bigint,
    approved_at timestamptz,
    created_at  timestamptz not null default now()
);


create table clientes (
    id         bigint generated always as identity primary key,
    nome       text        not null,
    descricao  text,
    ativo      boolean     not null default true,
    created_at timestamptz not null default now()
);


create table eventos (
    id          bigint generated always as identity primary key,
    nome        text        not null,
    descricao   text,
    data_inicio date        not null,
    data_fim    date        not null,
    created_at  timestamptz not null default now()
);


create table unavailability (
    id                  bigint generated always as identity primary key,
    user_id             bigint      not null references users5 (id) on delete cascade,
    full_name           text,
    unavailability_type text,        
    department          text,
    start_date          date        not null,
    end_date            date        not null,
    total_days          integer,
    status              text,       
    reviewed_by         bigint,
    reviewed_at         timestamptz,
    created_at          timestamptz not null default now()
);


create table user_clientes (
    user_id    bigint      not null references users5 (id)   on delete cascade,
    cliente_id bigint      not null references clientes (id)  on delete cascade,
    ativo      boolean     not null default true,
    created_at timestamptz not null default now(),
    primary key (user_id, cliente_id)
);


create table evento_clientes (
    evento_id  bigint not null references eventos (id)  on delete cascade,
    cliente_id bigint not null references clientes (id) on delete cascade,
    primary key (evento_id, cliente_id)
);


create index idx_unavailability_user_id   on unavailability (user_id);
create index idx_unavailability_status    on unavailability (status);
create index idx_user_clientes_cliente_id on user_clientes (cliente_id);
create index idx_evento_clientes_cliente  on evento_clientes (cliente_id);
