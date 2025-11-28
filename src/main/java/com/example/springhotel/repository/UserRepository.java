package com.example.springhotel.repository;

import com.example.springhotel.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Users findByUsername(String username); // ✅ méthode correcte
}

