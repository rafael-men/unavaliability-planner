package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Member> findByEmailInIgnoreCase(List<String> emails);
    List<Member> findAllByOrderByNameAsc();
}
