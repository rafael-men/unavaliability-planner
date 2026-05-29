package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.EventoDtos.ClienteRef;
import com.unavaliability.backend.dto.EventoDtos.EventoRequest;
import com.unavaliability.backend.dto.EventoDtos.EventoView;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Cliente;
import com.unavaliability.backend.models.Evento;
import com.unavaliability.backend.models.EventoCliente;
import com.unavaliability.backend.models.EventoClienteId;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.ClienteRepository;
import com.unavaliability.backend.repositories.EventoClienteRepository;
import com.unavaliability.backend.repositories.EventoRepository;
import com.unavaliability.backend.security.Roles;
import com.unavaliability.backend.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final EventoClienteRepository eventoClienteRepository;
    private final ClienteRepository clienteRepository;

    public EventoService(EventoRepository eventoRepository,
                         EventoClienteRepository eventoClienteRepository,
                         ClienteRepository clienteRepository) {
        this.eventoRepository = eventoRepository;
        this.eventoClienteRepository = eventoClienteRepository;
        this.clienteRepository = clienteRepository;
    }

    private void requireAdminEditor(User actor) {
        if (!Roles.isAdminEditor(actor.getRole())) {
            throw ApiException.forbidden("Apenas Admin Editor pode realizar esta ação.");
        }
    }


    @Transactional(readOnly = true)
    public List<EventoView> list() {
        return attachClientes(eventoRepository.findAllByOrderByDataInicioDesc());
    }


    @Transactional
    public EventoView create(User actor, EventoRequest req) {
        requireAdminEditor(actor);
        validate(req, null);
        Evento e = new Evento();
        e.setNome(TextUtils.cleanText(req.nome()));
        e.setDescricao(req.descricao() != null ? TextUtils.cleanText(req.descricao()) : null);
        e.setDataInicio(req.data_inicio());
        e.setDataFim(req.data_fim());
        e = eventoRepository.save(e);
        setEventoClientes(e.getId(), req.cliente_ids());
        return attachClientes(List.of(e)).get(0);
    }


    @Transactional
    public EventoView update(User actor, Long id, EventoRequest req) {
        requireAdminEditor(actor);
        Evento e = eventoRepository.findById(id).orElse(null);
        if (e == null) {
            throw ApiException.notFound("Evento não encontrado.");
        }
        LocalDate inicio = req.data_inicio() != null ? req.data_inicio() : e.getDataInicio();
        LocalDate fim = req.data_fim() != null ? req.data_fim() : e.getDataFim();
        String nome = req.nome() != null ? req.nome() : e.getNome();
        validateMerged(nome, inicio, fim, req.cliente_ids());

        if (req.nome() != null) {
            e.setNome(TextUtils.cleanText(req.nome()));
        }
        if (req.descricao() != null) {
            e.setDescricao(TextUtils.cleanText(req.descricao()));
        }
        if (req.data_inicio() != null) {
            e.setDataInicio(req.data_inicio());
        }
        if (req.data_fim() != null) {
            e.setDataFim(req.data_fim());
        }
        eventoRepository.save(e);
        if (req.cliente_ids() != null) {
            setEventoClientes(e.getId(), req.cliente_ids());
        }
        return attachClientes(List.of(e)).get(0);
    }


    @Transactional
    public void delete(User actor, Long id) {
        requireAdminEditor(actor);
        if (!eventoRepository.existsById(id)) {
            throw ApiException.notFound("Evento não encontrado.");
        }
        eventoClienteRepository.deleteByIdEventoId(id);
        eventoRepository.deleteById(id);
    }

    private void validate(EventoRequest req, Evento current) {
        if (req == null) {
            throw ApiException.badRequest("Nome do evento é obrigatório.");
        }
        validateMerged(req.nome(), req.data_inicio(), req.data_fim(), req.cliente_ids());
    }

    private void validateMerged(String nome, LocalDate inicio, LocalDate fim, List<Long> clienteIds) {
        if (nome == null || nome.trim().isEmpty()) {
            throw ApiException.badRequest("Nome do evento é obrigatório.");
        }
        if (inicio == null) {
            throw ApiException.badRequest("Data de início é obrigatória.");
        }
        if (fim == null) {
            throw ApiException.badRequest("Data de fim é obrigatória.");
        }
        if (fim.isBefore(inicio)) {
            throw ApiException.badRequest("Data de fim deve ser posterior ou igual à data de início.");
        }
        if (clienteIds != null && clienteIds.isEmpty()) {
            throw ApiException.badRequest("Selecione ao menos um cliente.");
        }
    }


    private void setEventoClientes(Long eventoId, List<Long> clienteIds) {
        eventoClienteRepository.deleteByIdEventoId(eventoId);
        if (clienteIds == null || clienteIds.isEmpty()) {
            return;
        }
        List<EventoCliente> rows = new ArrayList<>();
        for (Long cid : new HashSet<>(clienteIds)) {
            rows.add(new EventoCliente(new EventoClienteId(eventoId, cid)));
        }
        eventoClienteRepository.saveAll(rows);
    }


    private List<EventoView> attachClientes(List<Evento> eventos) {
        if (eventos.isEmpty()) {
            return List.of();
        }
        List<Long> ids = eventos.stream().map(Evento::getId).toList();
        List<EventoCliente> links = eventoClienteRepository.findByIdEventoIdIn(ids);

        Set<Long> clienteIds = new HashSet<>();
        Map<Long, List<Long>> linksByEvento = new HashMap<>();
        for (EventoCliente l : links) {
            Long evId = l.getId().getEventoId();
            Long clId = l.getId().getClienteId();
            clienteIds.add(clId);
            linksByEvento.computeIfAbsent(evId, k -> new ArrayList<>()).add(clId);
        }

        Map<Long, ClienteRef> clienteById = new HashMap<>();
        if (!clienteIds.isEmpty()) {
            for (Cliente c : clienteRepository.findAllById(clienteIds)) {
                clienteById.put(c.getId(), new ClienteRef(c.getId(), c.getNome()));
            }
        }

        List<EventoView> out = new ArrayList<>();
        for (Evento e : eventos) {
            List<Long> cids = linksByEvento.getOrDefault(e.getId(), List.of());
            List<ClienteRef> refs = new ArrayList<>();
            for (Long cid : cids) {
                ClienteRef ref = clienteById.get(cid);
                if (ref != null) {
                    refs.add(ref);
                }
            }
            out.add(EventoView.of(e, cids, refs));
        }
        return out;
    }
}
