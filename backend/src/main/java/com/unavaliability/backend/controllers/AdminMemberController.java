package com.unavaliability.backend.controllers;

import jakarta.validation.Valid;
import com.unavaliability.backend.dto.MemberDtos.MemberRequest;
import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.MemberService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberService memberService;
    private final CurrentUserProvider currentUser;

    public AdminMemberController(MemberService memberService, CurrentUserProvider currentUser) {
        this.memberService = memberService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Member> list() {
        currentUser.require();
        return memberService.listAll();
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody MemberRequest req) {
        Member m = memberService.create(currentUser.require(), req);
        return Map.of("success", true, "member", m);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody MemberRequest req) {
        Member m = memberService.update(currentUser.require(), id, req);
        return Map.of("success", true, "member", m);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        memberService.delete(currentUser.require(), id);
        return Map.of("success", true);
    }
}
