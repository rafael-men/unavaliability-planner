package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.AuthDtos.LoginRequest;
import com.unavaliability.backend.dto.AuthDtos.LoginResponse;
import com.unavaliability.backend.dto.AuthDtos.RegisterRequest;
import com.unavaliability.backend.dto.AuthDtos.UserSummary;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.JwtService;
import com.unavaliability.backend.security.Roles;
import com.unavaliability.backend.util.TextUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;


@Service
public class AuthService {

    private static final long WINDOW_MS = 15 * 60 * 1000;
    private static final int MAX_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SetorService setorService;

    /** count = tentativas na janela atual; reset = epoch ms em que a janela expira. */
    private record RateEntry(int count, long reset) {
    }

    private final ConcurrentHashMap<String, RateEntry> loginAttempts = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, MemberRepository memberRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, SetorService setorService) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.setorService = setorService;
    }


    public void checkLoginRate(String ip, long nowMs) {
        RateEntry entry = loginAttempts.compute(ip == null ? "unknown" : ip, (k, v) -> {
            if (v == null || nowMs > v.reset()) {
                return new RateEntry(1, nowMs + WINDOW_MS);
            }
            return new RateEntry(v.count() + 1, v.reset());
        });
        if (entry.count() > MAX_ATTEMPTS) {
            long waitMin = (long) Math.ceil((entry.reset() - nowMs) / 1000.0 / 60.0);
            throw ApiException.tooManyRequests(
                    "Muitas tentativas de login. Tente novamente em " + waitMin + " minuto(s).");
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        if (req == null || isBlank(req.email()) || isBlank(req.password())) {
            throw ApiException.badRequest("Email e senha obrigatórios.");
        }
        User user = userRepository.findByEmailIgnoreCase(req.email().toLowerCase().trim()).orElse(null);
        if (user == null) {
            throw ApiException.unauthorized("Email ou senha incorretos.");
        }
        if (user.getPassw() == null || !passwordEncoder.matches(req.password(), user.getPassw())) {
            throw ApiException.unauthorized("Email ou senha incorretos.");
        }
        String status = user.getStatus() == null ? "approved" : user.getStatus();
        if ("pending".equals(status)) {
            throw ApiException.forbidden("Seu cadastro está aguardando aprovação de um administrador.");
        }
        if ("rejected".equals(status)) {
            throw ApiException.forbidden("Seu cadastro foi rejeitado. Entre em contato com um administrador.");
        }
        String token = jwtService.generateToken(user);
        return new LoginResponse(true, token,
                new UserSummary(user.getId(), user.getEmail(), user.getNome(), user.getRole()));
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (req == null || isBlank(req.email()) || isBlank(req.password())
                || isBlank(req.full_name()) || isBlank(req.department())) {
            throw ApiException.badRequest("Todos os campos são obrigatórios.");
        }
        String emailLower = req.email().toLowerCase().trim();
        if (!TextUtils.isValidEmail(emailLower)) {
            throw ApiException.badRequest("Email inválido.");
        }
        if (!setorService.exists(req.department())) {
            throw ApiException.badRequest("Setor inválido.");
        }
        if (req.password().length() < 6) {
            throw ApiException.badRequest("Senha deve ter pelo menos 6 caracteres.");
        }
        if (userRepository.existsByEmailIgnoreCase(emailLower)) {
            throw ApiException.badRequest("Este email já está cadastrado.");
        }

        String hash = passwordEncoder.encode(req.password());
        Member member = memberRepository.findByEmailIgnoreCase(emailLower).orElse(null);
        if (member == null) {
            member = new Member();
            member.setName(TextUtils.cleanText(req.full_name()));
            member.setEmail(emailLower);
            member.setArea(req.department());
            member.setOperacoes(true);
            member.setDayOffsQuota(20);
            member = memberRepository.save(member);
        }

        User user = new User();
        user.setEmail(emailLower);
        user.setPassw(hash);
        user.setNome(TextUtils.cleanText(req.full_name()));
        user.setDepartment(req.department());
        user.setMemberId(member.getId());
        user.setRole(Roles.COLABORADOR);
        user.setStatus("pending");
        userRepository.save(user);
    }

    public UserSummary me(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getNome(), user.getRole());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
