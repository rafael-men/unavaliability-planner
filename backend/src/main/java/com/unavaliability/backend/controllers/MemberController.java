package com.unavaliability.backend.controllers;

import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;


@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final CurrentUserProvider currentUser;

    public MemberController(MemberService memberService, CurrentUserProvider currentUser) {
        this.memberService = memberService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Member> list() {
        return memberService.listAll(currentUser.require());
    }

    @GetMapping("/me")
    public MemberService.MemberMeView me() {
        return memberService.me(currentUser.require());
    }

    @GetMapping("/by-email/{email}")
    public MemberService.MemberByEmailView byEmail(@PathVariable String email) {
       User actor = currentUser.require();
       String emailDec = URLDecoder.decode(email, StandardCharsets.UTF_8);
       return memberService.byEmail(actor, emailDec);
    }
}
