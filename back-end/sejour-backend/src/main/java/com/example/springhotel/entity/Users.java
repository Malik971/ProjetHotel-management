package com.example.springhotel.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String telephone;

    private String password;
    private boolean enabled;

    /**
     * Identifiant unique emis par Keycloak pour cet utilisateur.
     * <p>
     * Vaut null pour les comptes crees via le flux JWT maison
     * (inscription classique, comptes de demo seedes au demarrage).
     * Est renseigne automatiquement par KeycloakUserProvisioningService
     * lors de la premiere connexion via Keycloak (JIT provisioning).
     * <p>
     * Format : UUID Keycloak, ex : d9bc9f69-ab0b-4955-b578-f1d1b2d904cd.
     * Contrainte UNIQUE en base (voir migration V8), nullable.
     *
     * @see com.example.springhotel.security.oauth2.KeycloakUserProvisioningService
     */
    @Column(name = "keycloak_sub", unique = true)
    private String keycloakSub;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "users_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

    // Methode utilitaire pour ajouter un role
    public void addRole(Role role) {
        this.roles.add(role);
    }
}