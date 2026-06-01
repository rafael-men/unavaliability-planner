package com.unavaliability.backend.controllers;

import com.unavaliability.backend.dto.UserDtos.AssignSetorRequest;
import com.unavaliability.backend.dto.UserDtos.ChangeRoleRequest;
import com.unavaliability.backend.dto.UserDtos.CreateUserRequest;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.UserAdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final CurrentUserProvider currentUser;

    public AdminUserController(UserAdminService userAdminService, CurrentUserProvider currentUser) {
        this.userAdminService = userAdminService;
        this.currentUser = currentUser;
    }

    @GetMapping("/users")
    public List<User> listUsers() {
        return userAdminService.listAll(currentUser.require());
    }

    @GetMapping("/pending")
    public List<User> listPending() {
        return userAdminService.listPending(currentUser.require());
    }

    @PostMapping("/users/create")
    public Map<String, Object> create(@RequestBody CreateUserRequest req) {
        userAdminService.createUser(currentUser.require(), req);
        return Map.of("success", true, "message", "Usuário criado e aprovado com sucesso.");
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        userAdminService.deleteUser(currentUser.require(), id);
        return Map.of("success", true);
    }

    @PostMapping("/users/{id}/assign-setor")
    public Map<String, Object> assignSetor(@PathVariable Long id, @RequestBody AssignSetorRequest req) {
        userAdminService.assignSetor(currentUser.require(), id, req);
        return Map.of("success", true);
    }

    @PostMapping("/approve/{id}")
    public Map<String, Object> approve(@PathVariable Long id) {
        userAdminService.approveUser(currentUser.require(), id);
        return Map.of("success", true);
    }

    @PostMapping("/reject/{id}")
    public Map<String, Object> reject(@PathVariable Long id) {
        userAdminService.rejectUser(currentUser.require(), id);
        return Map.of("success", true);
    }

    @PostMapping("/change-role/{id}")
    public Map<String, Object> changeRole(@PathVariable Long id, @RequestBody ChangeRoleRequest req) {
        userAdminService.changeRole(currentUser.require(), id, req);
        return Map.of("success", true);
    }
}
