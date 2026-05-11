package com.example.springhotel.integration.pastell.config;

import com.example.springhotel.integration.pastell.security.PastellCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration dediee au bean {@link PastellCredentialsProvider} (Lot 6).
 *
 * <b>Pourquoi un fichier separe et pas dans {@link PastellConfig} ?</b>
 *
 * {@link PastellConfig} a besoin d'injecter ce provider par champ
 * ({@code @Autowired(required = false)}) pour selectionner l'interceptor
 * d'authentification (rotatif si present, statique sinon). Si le bean etait
 * declare DANS PastellConfig, Spring tomberait sur une reference circulaire :
 * <pre>
 *   PastellConfig (construction) → @Autowired credentialsProvider
 *                               → besoin d'invoquer @Bean pastellCredentialsProvider()
 *                               → necessite un PastellConfig deja construit
 *                               → cycle, BeanCurrentlyInCreationException
 * </pre>
 *
 * En placant le bean dans une classe @Configuration distincte, Spring construit
 * d'abord PastellCredentialsConfig (qui ne depend de rien d'autre que de
 * PastellProperties), puis injecte le bean dans PastellConfig sans cycle.
 *
 * <b>Mode rotatif active uniquement si :</b>
 * <ul>
 *   <li>{@code pastell.enabled=true} (sinon Pastell est totalement desactive)</li>
 *   <li>{@code pastell.master-secret} est defini et non vide</li>
 * </ul>
 *
 * En mode statique (master-secret absent), aucun bean n'est cree et l'injection
 * par champ dans {@link PastellConfig} laisse le champ a null.
 */
@Configuration
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellCredentialsConfig {

    private static final Logger log = LoggerFactory.getLogger(PastellCredentialsConfig.class);

    /**
     * Bean {@link PastellCredentialsProvider}, declare uniquement quand le
     * master-secret est configure (mode rotatif Lot 6).
     */
    @Bean
    @ConditionalOnProperty(name = "pastell.master-secret")
    public PastellCredentialsProvider pastellCredentialsProvider(PastellProperties properties) {
        log.info("Mode auth Pastell sortante : ROTATIF (HMAC-SHA256, rotation quotidienne UTC).");
        return new PastellCredentialsProvider(properties.getMasterSecret());
    }
}