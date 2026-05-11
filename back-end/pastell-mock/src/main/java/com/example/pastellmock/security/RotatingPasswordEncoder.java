package com.example.pastellmock.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder qui accepte deux mots de passe valides simultanement :
 * celui d'aujourd'hui (UTC) et celui d'hier.
 * <p>
 * <b>Pourquoi cette tolerance ?</b> Au passage de minuit UTC, sejour-backend et
 * pastell-mock changent de jour. Sans synchronisation explicite, il est possible
 * que pendant quelques secondes (le temps de la requete HTTP en vol, ou un
 * leger drift d'horloge entre deux conteneurs Render) sejour envoie encore le
 * password d'hier alors que le mock attend deja celui d'aujourd'hui. Sans
 * tolerance, ces secondes produisent des 401 qui declencheraient inutilement
 * le retry du Lot 4.
 * <p>
 * <b>Trade-off securite :</b> un mot de passe leake reste valide pendant 24h
 * apres son expiration "officielle". Pour ce projet portfolio, c'est acceptable.
 * Pour un usage production, la rotation horaire (et non journaliere) suffirait
 * a reduire ce risque.
 * <p>
 * <b>Convention de stockage :</b> ce PasswordEncoder est concu pour fonctionner
 * avec un UserDetails qui ne stocke PAS de password reel. Le champ password du
 * UserDetails est ignore. Seules les valeurs calculees par {@link MockCredentialsProvider}
 * comptent. La methode {@link #encode} est utilisee uniquement au boot par Spring
 * pour normaliser le password "stocke", elle retourne donc une chaine fixe sentinelle.
 */
public class RotatingPasswordEncoder implements PasswordEncoder {

    private static final Logger log = LoggerFactory.getLogger(RotatingPasswordEncoder.class);

    /** Valeur sentinelle qui occupe le champ password du UserDetails. Jamais comparee directement. */
    private static final String SENTINEL = "{rotating-handled-by-encoder}";

    private final MockCredentialsProvider provider;

    public RotatingPasswordEncoder(MockCredentialsProvider provider) {
        this.provider = provider;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        // Appele une seule fois au boot pour normaliser le password "stocke".
        // On retourne la sentinelle : le vrai check se fait dans matches().
        return SENTINEL;
    }

    /**
     * Compare le mot de passe brut envoye par le client a today et yesterday.
     * Le parametre encodedPassword (la sentinelle) est ignore.
     * <p>
     * Comparaison en temps constant via {@link String#equals} : suffisant pour
     * notre modele de menace (pas de timing attack distant a craindre sur un
     * mock de demo).
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            return false;
        }
        String raw = rawPassword.toString();
        String today = provider.getCurrentPassword();
        if (raw.equals(today)) {
            return true;
        }
        String yesterday = provider.getYesterdayPassword();
        if (raw.equals(yesterday)) {
            log.debug("Mock auth : password d'hier accepte (tolerance changement de jour UTC).");
            return true;
        }
        return false;
    }
}
