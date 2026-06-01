package com.unavaliability.backend.controllers;

import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.SetorService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin/setores")
public class AdminSetorController {

    private final SetorService setorService;
    private final CurrentUserProvider currentUser;

    public AdminSetorController(SetorService setorService, CurrentUserProvider currentUser) {
        this.setorService = setorService;
        this.currentUser = currentUser;
    }

    public record SetorRequest(String name) {
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody SetorRequest req) {
        List<String> setores = setorService.add(currentUser.require(), req == null ? null : req.name());
        return Map.of("success", true, "setores", setores);
    }

    @PutMapping("/{index}")
    public Map<String, Object> update(@PathVariable int index, @RequestBody SetorRequest req) {
        List<String> setores = setorService.update(currentUser.require(), index, req == null ? null : req.name());
        return Map.of("success", true, "setores", setores);
    }

    @DeleteMapping("/{index}")
    public Map<String, Object> remove(@PathVariable int index) {
        List<String> setores = setorService.remove(currentUser.require(), index);
        return Map.of("success", true, "setores", setores);
    }
}
