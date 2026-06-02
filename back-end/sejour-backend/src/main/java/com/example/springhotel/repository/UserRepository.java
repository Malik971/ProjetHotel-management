package com.example.springhotel.repository;

import com.example.springhotel.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Recherche un profil utilisateur par son identifiant Keycloak (claim "sub").
     * <p>
     * Utilise par KeycloakUserProvisioningService pour retrouver le profil
     * local d'un utilisateur authentifie via Keycloak, sans passer par l'email
     * (qui peut ne pas etre present ou different entre Keycloak et la base).
     * <p>
     * Retourne empty si aucun profil n'est associe a ce sub, ce qui declenche
     * la creation du profil (JIT provisioning).
     *
     * @param keycloakSub le claim "sub" du token Keycloak, format UUID
     * @return le profil utilisateur associe, ou empty si absent
     */
    Optional<Users> findByKeycloakSub(String keycloakSub);
}