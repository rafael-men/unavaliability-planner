package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.ClienteDtos.AssignUserRequest;
import com.unavaliability.backend.dto.ClienteDtos.ClienteRequest;
import com.unavaliability.backend.dto.ClienteDtos.UserClienteLink;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Cliente;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.models.UserCliente;
import com.unavaliability.backend.models.UserClienteId;
import com.unavaliability.backend.repositories.ClienteRepository;
import com.unavaliability.backend.repositories.UserClienteRepository;
import com.unavaliability.backend.security.Roles;
import com.unavaliability.backend.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UserClienteRepository userClienteRepository;

    public ClienteService(ClienteRepository clienteRepository, UserClienteRepository userClienteRepository) {
        this.clienteRepository = clienteRepository;
        this.userClienteRepository = userClienteRepository;
    }

    private void requireAdminEditor(User actor) {
        if (!Roles.isAdminEditor(actor.getRole())) {
            throw ApiException.forbidden("Apenas Admin Editor pode realizar esta ação.");
        }
    }


    @Transactional(readOnly = true)
    public ClientesResponse listWithLinks(User actor) {
        requireAdminEditor(actor);
        List<Cliente> clientes = clienteRepository.findAllByOrderByNomeAsc();
        List<UserClienteLink> links = userClienteRepository.findAll().stream()
                .map(uc -> new UserClienteLink(
                        uc.getId().getUserId(), uc.getId().getClienteId(), uc.getAtivo()))
                .toList();
        return new ClientesResponse(clientes, links);
    }


    @Transactional
    public Cliente create(User actor, ClienteRequest req) {
        requireAdminEditor(actor);
        validateNome(req == null ? null : req.nome());
        String nome = TextUtils.cleanText(req.nome());
        if (clienteRepository.findByNomeIgnoreCase(nome).isPresent()) {
            throw ApiException.badRequest("Já existe um cliente com este nome.");
        }
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setDescricao(req.descricao() != null ? TextUtils.cleanText(req.descricao()) : null);
        c.setAtivo(req.ativo() == null ? true : TextUtils.toBool(req.ativo()));
        return clienteRepository.save(c);
    }


    @Transactional
    public Cliente update(User actor, Long id, ClienteRequest req) {
        requireAdminEditor(actor);
        Cliente c = clienteRepository.findById(id).orElse(null);
        if (c == null) {
            throw ApiException.notFound("Cliente não encontrado.");
        }
        if (req != null && req.nome() != null) {
            validateNome(req.nome());
            c.setNome(TextUtils.cleanText(req.nome()));
        }
        if (req != null && req.descricao() != null) {
            c.setDescricao(TextUtils.cleanText(req.descricao()));
        }
        if (req != null && req.ativo() != null) {
            c.setAtivo(TextUtils.toBool(req.ativo()));
        }
        return clienteRepository.save(c);
    }


    @Transactional
    public void delete(User actor, Long id) {
        requireAdminEditor(actor);
        if (!clienteRepository.existsById(id)) {
            throw ApiException.notFound("Cliente não encontrado.");
        }
        clienteRepository.deleteById(id);
    }


    @Transactional
    public void assign(User actor, Long clienteId, AssignUserRequest req) {
        requireAdminEditor(actor);
        if (req == null || req.user_id() == null) {
            throw ApiException.badRequest("user_id inválido.");
        }
        boolean ativo = req.ativo() == null || TextUtils.toBool(req.ativo());
        if (!ativo) {
            unassign(actor, clienteId, req.user_id());
            return;
        }
        UserClienteId key = new UserClienteId(req.user_id(), clienteId);
        UserCliente uc = userClienteRepository.findById(key).orElse(new UserCliente(key, true));
        uc.setAtivo(true);
        userClienteRepository.save(uc);
    }


    @Transactional
    public void unassign(User actor, Long clienteId, Long userId) {
        requireAdminEditor(actor);
        if (userId == null) {
            throw ApiException.badRequest("user_id inválido.");
        }
        userClienteRepository.deleteById(new UserClienteId(userId, clienteId));
    }

    private void validateNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw ApiException.badRequest("Nome do cliente é obrigatório.");
        }
        if (nome.trim().length() < 2) {
            throw ApiException.badRequest("Nome do cliente deve ter ao menos 2 caracteres.");
        }
    }

    public record ClientesResponse(List<Cliente> clientes, List<UserClienteLink> links) {
    }
}
