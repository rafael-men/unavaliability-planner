package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByStatus(String status);
    List<User> findByRoleInAndDepartmentAndStatus(List<String> roles, String department, String status);
    @Query("select u from User u where u.role = 'lider' and u.department = :department and u.status = 'approved'")
    List<User> findLideresByDepartment(@Param("department") String department);
}
