-- Impede, no banco, que um mesmo usuário tenha dois períodos pendentes/aprovados
-- que se sobrepõem. A checagem em Java (checkOverlap) continua existindo para
-- dar uma mensagem amigável, mas não protege contra requisições simultâneas.

create extension if not exists btree_gist;

alter table unavailability
    add constraint unav_no_overlap
    exclude using gist (
        user_id with =,
        daterange(start_date, end_date, '[]') with &&
    )
    where (status in ('pending', 'approved'));
