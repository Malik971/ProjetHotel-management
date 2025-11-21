package com.example.springhotel.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private int valeur;
}
