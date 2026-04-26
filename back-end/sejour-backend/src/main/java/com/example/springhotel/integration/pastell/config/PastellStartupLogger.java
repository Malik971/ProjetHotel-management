package com.example.springhotel.integration.pastell.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Log un banner clair au demarrage de l'application pour indiquer
 * l'etat de l'integration Pastell.
 *
 * Contrairement a {@link PastellConfig}, ce composant est TOUJOURS charge,
 * meme quand l'integration est desactivee. Objectif : qu'un developpeur
 * qui clone le repo comprenne en 2 secondes si Pastell est actif ou non,
 * sans avoir a chercher dans les properties.
 *
 * DevRel note : ce type de banner reduit enormement les questions de support
 * du type "est-ce que l'integration tourne bien ?". Le premier log qu'on regarde
 * doit repondre.
 */
@Configuration
@EnableConfigurationProperties(PastellProperties.class)
public class PastellStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(PastellStartupLogger.class);

    private final PastellProperties properties;

    public PastellStartupLogger(PastellProperties properties) {
        this.properties = properties;
    }

    /**
     * Ecoute ApplicationReadyEvent plutot que @PostConstruct pour que le banner
     * apparaisse APRES tous les autres logs de demarrage Spring Boot. Il est ainsi
     * visible en fin de sequence, la ou l'oeil du developpeur va naturellement.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logPastellStatus() {
        if (!properties.isEnabled()) {
            log.info("");
            log.info("===== Pastell Integration =====");
            log.info("Status : DISABLED");
            log.info("        (pour activer : pastell.enabled=true dans application.properties)");
            log.info("===============================");
            log.info("");
            return;
        }

        log.info("");
        log.info("===== Pastell Integration =====");
        log.info("Status    : ENABLED");
        log.info("Mode      : {}", properties.getMode());
        log.info("URL       : {}", properties.getUrl());
        log.info("Entity ID : {}", properties.getEntiteId());
        log.info("Type      : {}", properties.getTypeDossier());
        log.info("Timeout   : {}ms", properties.getTimeoutMs());
        log.info("Webhook   : {}",
                properties.getWebhook().isEnabled()
                        ? "ENABLED (endpoint /api/integration/pastell/webhook actif)"
                        : "DISABLED (polling seul)");
        log.info("Polling   : toutes les {}ms", properties.getPolling().getIntervalMs());
        log.info("===============================");
        log.info("");
    }
}