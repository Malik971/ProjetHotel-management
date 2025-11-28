package com.example.springhotel.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")   // ✔ guillemets obligatoires
public class Users {     // ✔ nom de classe en majuscule

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String username;
    private String password;
    private String role;

    // ✔ Constructeur vide obligatoire pour JPA
    public Users() {}

    // ✔ Getters / Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }


    public Boolean getRoles() {
        return null;
    }
}
