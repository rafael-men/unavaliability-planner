
insert into members (name, email, area, operacoes, day_offs_quota)
values ('Rafael (Admin)', 'rafael@gmail.com', 'Tecnologia', true, 20)
on conflict (email) do nothing;


insert into users5 (email, nome, passw, role, status, department, member_id, approved_at)
values (
    'rafael@gmail.com',
    'Rafael (Admin)',
    '$2a$10$a2RcODkLfmOzLEuupTOo/eqHGtOOuD7VS09DTejxq4aqBaZfkrv9C',
    'admin_master',
    'approved',
    'Tecnologia',
    (select id from members where email = 'rafael@gmail.com'),
    now()
)
on conflict (email) do nothing;
