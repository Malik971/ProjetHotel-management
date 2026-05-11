package com.example.springhotel.integration.pastell.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Derive les credentials HTTP Basic Pastell a partir d'un secret maitre partage.
 * <p>
 * Cote sejour-backend, ce provider est appele a chaque requete sortante par
 * {@link RotatingBasicAuthInterceptor} pour calculer le mot de passe du jour
 * sans coordination explicite avec le mock.
 * <p>
 * <b>Principe :</b> les deux services (sejour-backend et pastell-mock) lisent
 * la meme variable d'environnement {@code PASTELL_MASTER_SECRET}. A partir de
 * ce secret, ils calculent independamment le meme username (stable) et le meme
 * mot de passe (rotatif quotidien). Aucune requete ne transite entre eux pour
 * synchroniser ces valeurs : la derivation est strictement deterministe.
 * <p>
 * <b>Algorithme :</b>
 * <pre>
 *   username = "sejour-" + hex(HMAC-SHA256(master_secret, "username"))[0..16]
 *   password = base64url(HMAC-SHA256(master_secret, "password:" + UTC_DATE))[0..32]
 * </pre>
 * <p>
 * Le username ne tourne PAS avec la date. Raison : Spring Security cherche un
 * UserDetails par username avant de verifier le password. Si le username changeait
 * chaque jour, le mock devrait gerer une rotation cote UserDetailsService, ce qui
 * complique inutilement le code pour un benefice marginal. Seul le mot de passe
 * porte la fraicheur temporelle.
 * <p>
 * <b>Securite :</b> ce mecanisme n'est pas une defense crypto serieuse contre un
 * attaquant qui aurait acces au secret maitre. C'est un mecanisme de demo qui
 * elimine les credentials en clair dans la configuration Render et qui demontre
 * une rotation automatique. Voir {@code CREDENTIALS.md} pour les details.
 */
public class PastellCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(PastellCredentialsProvider.class);

    /** Algorithme HMAC. SHA-256 est le standard, disponible partout sans dependance externe. */
    private static final String HMAC_ALGO = "HmacSHA256";

    /** Longueur du username (apres le prefixe "sejour-"). 16 hex chars = 64 bits, suffisant. */
    private static final int USERNAME_HEX_LENGTH = 16;

    /** Longueur du password derive. 32 chars base64 = 192 bits effectifs. */
    private static final int PASSWORD_LENGTH = 32;

    /** Le secret maitre lu depuis l'environnement. Jamais logue. */
    private final byte[] masterSecret;

    /**
     * @param masterSecret le secret partage entre sejour-backend et pastell-mock.
     *                     Typiquement injecte via la propriete {@code pastell.master-secret}
     *                     qui pointe sur la variable d'environnement {@code PASTELL_MASTER_SECRET}.
     * @throws IllegalArgumentException si le secret est null ou vide
     */
    public PastellCredentialsProvider(String masterSecret) {
        if (masterSecret == null || masterSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "PASTELL_MASTER_SECRET est requis pour activer la rotation des credentials. " +
                            "Generez-en un avec : openssl rand -hex 32");
        }
        this.masterSecret = masterSecret.getBytes(StandardCharsets.UTF_8);
        log.info("PastellCredentialsProvider initialise (rotation quotidienne UTC active).");
    }

    /**
     * Username stable, derive du secret maitre uniquement (pas de date).
     * Toujours identique entre redemarrages tant que le secret ne change pas.
     */
    public String getUsername() {
        byte[] hmac = computeHmac("username");
        String hex = HexFormat.of().formatHex(hmac).substring(0, USERNAME_HEX_LENGTH);
        return "sejour-" + hex;
    }

    /**
     * Mot de passe du jour, derive du secret maitre et de la date UTC courante.
     * Change automatiquement a minuit UTC.
     */
    public String getCurrentPassword() {
        return passwordForDate(LocalDate.now(ZoneOffset.UTC));
    }

    /**
     * Mot de passe pour une date donnee. Utilise par le mock pour accepter
     * aussi le mot de passe de la veille (grace window au changement de jour).
     */
    public String passwordForDate(LocalDate utcDate) {
        byte[] hmac = computeHmac("password:" + utcDate.toString());
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        return b64.substring(0, PASSWORD_LENGTH);
    }

    /**
     * Calcule HMAC-SHA256(masterSecret, data).
     * Encapsule les checked exceptions internes pour rester ergonomique a l'appel.
     */
    private byte[] computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(masterSecret, HMAC_ALGO));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // HmacSHA256 fait partie de la JCE standard, ne devrait jamais lever.
            // Si ca arrive, c'est une regression d'environnement, on echoue net.
            throw new IllegalStateException("HMAC-SHA256 indisponible dans la JVM", e);
        }
    }
}
