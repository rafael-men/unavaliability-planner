package com.unavaliability.backend.util;


import java.time.OffsetDateTime;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.Roles;

import jakarta.transaction.Transactional;

@Component 
public class InitialAdminBootstrap implements ApplicationRunner{
   static final int MIN_PASSWORD_LENGTH = 12;
   static final String DEFAULT_DEPARTMENT = "Tecnologia";

   private static final Logger logger = LoggerFactory.getLogger(InitialAdminBootstrap.class);
   private final UserRepository userRepository;
   private final MemberRepository memberRepository;
   private final PasswordEncoder passwordEncoder;
   private final String adminEmail;
   private final String adminPassword;
   private final String adminName;

   public InitialAdminBootstrap(UserRepository userRepository,MemberRepository memberRepository, PasswordEncoder passwordEncoder,
      @Value("${ADMIN_EMAIL:}") String adminEmail,
      @Value("${ADMIN_PASSWORD:}") String adminPassword,
      @Value("${INITIAL_ADMIN_NAME:Administrador}") String adminName )  {
          this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail == null ? "" : adminEmail.strip();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
        this.adminName = adminName == null || adminName.isBlank() ? "Administrador" : adminName.strip();
      }

   @Override
   @Transactional
   public void run(ApplicationArguments args) throws Exception {
      if (userRepository.count() > 0) {
            return;
        }

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            logger.warn("users5 está vazia, mas INITIAL_ADMIN ou ADMIN_PASSWORD não estão definidos. "
                    + "Nenhum administrador inicial foi criado.");
            return;
        }

        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }

        String email = adminEmail.toLowerCase(Locale.ROOT);

        Member member = memberRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Member m = new Member();
            m.setName(adminName);
            m.setEmail(email);
            m.setArea(DEFAULT_DEPARTMENT);
            m.setOperacoes(true);
            m.setDayOffsQuota(20);
            return memberRepository.save(m);
        });

        User admin = new User();
        admin.setEmail(email);
        admin.setNome(adminName);
        admin.setPassw(passwordEncoder.encode(adminPassword));
        admin.setRole(Roles.ADMIN_MASTER);
        admin.setStatus("approved");
        admin.setDepartment(DEFAULT_DEPARTMENT);
        admin.setMemberId(member.getId());
        admin.setApprovedAt(OffsetDateTime.now());
        userRepository.save(admin);

        logger.info("Administrador inicial criado: {}", email);
   }
}
