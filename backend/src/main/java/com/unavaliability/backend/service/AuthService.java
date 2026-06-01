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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;


@Service
public class AuthService {

    private static final long WINDOW_MS = 15 * 60 * 1000;
    private static final int MAX_ATTEMPTS = 10;
    private static final int MIN_PASSWORD_LEN = 6;

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SetorService setorService;
    private final int lockoutThreshold;
    private final long lockoutMs;

    private record RateEntry(int count, long reset) {
    }
    private record AccountEntry(int fails, long lockedUntil) {
    }

    private final ConcurrentHashMap<String, RateEntry> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AccountEntry> accountLocks = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, MemberRepository memberRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, SetorService setorService,
                       @Value("${app.security.lockout.threshold:5}") int lockoutThreshold,
                       @Value("${app.security.lockout.minutes:15}") long lockoutMinutes) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.setorService = setorService;
        this.lockoutThreshold = lockoutThreshold;
        this.lockoutMs = lockoutMinutes * 60 * 1000;
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
        long now = System.currentTimeMillis();
        if (req == null || isBlank(req.email()) || isBlank(req.password())) {
            throw ApiException.badRequest("Email e senha obrigatórios.");
        }
        String email = req.email().toLowerCase().trim();
        if (!TextUtils.isValidEmail(email)) {
            throw ApiException.unauthorized("Email ou senha incorretos.");
        }
        checkAccountLock(email, now);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            registerFailure(email, now);
            throw ApiException.unauthorized("Email ou senha incorretos.");
        }
        if (user.getPassw() == null || !passwordEncoder.matches(req.password(), user.getPassw())) {
            registerFailure(email, now);
            throw ApiException.unauthorized("Email ou senha incorretos.");
        }

        accountLocks.remove(email);

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


    private void checkAccountLock(String email, long nowMs) {
        AccountEntry e = accountLocks.get(email);
        if (e != null && e.lockedUntil() > nowMs) {
            long waitMin = (long) Math.ceil((e.lockedUntil() - nowMs) / 1000.0 / 60.0);
            throw ApiException.tooManyRequests(
                    "Conta temporariamente bloqueada por excesso de tentativas. "
                            + "Tente novamente em " + waitMin + " minuto(s).");
        }
    }


    private void registerFailure(String email, long nowMs) {
        accountLocks.compute(email, (k, v) -> {
            int fails = (v == null || v.lockedUntil() > 0 && v.lockedUntil() <= nowMs) ? 1 : v.fails() + 1;
            long lockedUntil = fails >= lockoutThreshold ? nowMs + lockoutMs : 0;
            return new AccountEntry(fails, lockedUntil);
        });
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
        validatePasswordStrength(req.password());
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

    
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LEN) {
            throw ApiException.badRequest("Senha deve ter pelo menos " + MIN_PASSWORD_LEN + " caracteres.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw ApiException.badRequest("Senha deve conter ao menos uma letra e um número.");
        }
    }

   
    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        loginAttempts.entrySet().removeIf(e -> now > e.getValue().reset());
        accountLocks.entrySet().removeIf(e -> {
            AccountEntry v = e.getValue();
            return v.lockedUntil() == 0 || v.lockedUntil() <= now;
        });
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
