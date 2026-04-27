package com.example.pastellmock.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint {@code GET /api/version.php} : handshake du mock Pastell.
 *
 * Le vrai Pastell expose un endpoint similaire qui retourne la version de
 * l'instance et quelques metadonnees. Cote client (sejour-backend), on s'en
 * sert pour verifier au demarrage que la plateforme cible est joignable et
 * qu'elle parle bien le protocole attendu.
 *
 * Cet endpoint est volontairement ANONYME (cf. MockSecurityConfig) : il sert
 * aussi de "healthcheck" pour les sondes type UptimeRobot ou les pipelines CI.
 *
 * Format de reponse approximatif (a affiner quand on aura une capture de la
 * vraie API Pastell sous les yeux) :
 * <pre>
 * {
 *   "name": "Pastell",
 *   "version": "3.0-mock",
 *   "edition": "mock"
 * }
 * </pre>
 *
 * Le champ {@code edition: "mock"} est un marqueur volontaire qui permet
 * d'identifier sans ambiguite qu'on parle au mock et pas a une instance reelle.
 * Il sera affiche dans les dashboards d'observabilite (Lot 6) pour eviter
 * tout doute sur l'environnement cible.
 */
@RestController
@RequestMapping("/api")
public class VersionController {

    private static final String PASTELL_NAME = "Pastell";
    private static final String MOCK_VERSION = "3.0-mock";
    private static final String EDITION_MARKER = "mock";

    @GetMapping("/version.php")
    public Map<String, String> version() {
        // LinkedHashMap pour garantir l'ordre dans la reponse JSON
        // (purement cosmetique, mais facilite le diff visuel en cas de comparaison
        // avec la reponse d'un Pastell reel)
        Map<String, String> response = new LinkedHashMap<>();
        response.put("name", PASTELL_NAME);
        response.put("version", MOCK_VERSION);
        response.put("edition", EDITION_MARKER);
        return response;
    }
}