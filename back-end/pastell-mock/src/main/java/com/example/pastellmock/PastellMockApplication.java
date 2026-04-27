package com.example.pastellmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree du mock Pastell.
 *
 * Cette application simule une instance Pastell reelle pour le developpement
 * local et la CI. Elle expose les memes endpoints HTTP que la vraie API Pastell
 * (form-data en entree, JSON en sortie, HTTP Basic auth) afin que le client
 * cote sejour-backend puisse etre teste sans dependance externe.
 *
 * Demarrage par defaut sur le port 8090 (cf. application.properties), pour
 * cohabiter avec sejour-backend qui ecoute sur 8080.
 *
 * IMPORTANT : ce mock est volontairement stateless en memoire au Paquet 1.
 * Le store en memoire des dossiers Pastell sera ajoute au Paquet suivant
 * (create-document.php, detail-document.php, change-action.php).
 */
@SpringBootApplication
public class PastellMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(PastellMockApplication.class, args);
    }
}