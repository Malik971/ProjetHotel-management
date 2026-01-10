package com.example.springhotel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Collection;

@Data
@Entity
public class Privilege {

    // Getters et setters
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "privileges")
    @JsonIgnore // Evite la boucle infinie
    private Collection<Role> roles;

    public Privilege() {}
    public Privilege(String name) { this.name = name; }

}
