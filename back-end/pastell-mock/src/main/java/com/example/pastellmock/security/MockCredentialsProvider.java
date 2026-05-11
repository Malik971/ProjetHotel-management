package com.example.pastellmock.security;

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
 * Pendant cote pastell-mock du {@code PastellCredentialsProvider} de sejour-backend.
 * <p>
 * Memes algorithmes, memes constantes : c'est l'identite des deux qui garantit
 * que sejour calcule et envoie le password que le mock attend. Toute divergence
 * (longueur, prefixe, algo, encodage) casse l'authentification.
 * <p>
 * <b>Pourquoi du code duplique plutot qu'un module commun ?</b> Un module Maven
 * partage couterait plus en complexite (un troisieme module, des dependances
 * croisees) qu'il n'apporte. 50 lignes dupliquees avec une regle "modifier les
 * deux en miroir" valent mieux qu'une abstraction premature. Les tests de
 * non-regression croises (executes au demarrage en INFO log) attrapent toute
 * divergence rapidement.
 * <p>
 * Specificite cote mock : ce provider expose une methode {@link #passwordForDate}
 * qui permet de calculer aussi le mot de passe d'hier, utile pour la fenetre
 * de tolerance au passage de minuit UTC (voir {@link RotatingPasswordEncoder}).
 */
public class MockCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(MockCredentialsProvider.class);

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final int USERNAME_HEX_LENGTH = 16;
    private static final int PASSWORD_LENGTH = 32;

    private final byte[] masterSecret;

    public MockCredentialsProvider(String masterSecret) {
        if (masterSecret == null || masterSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "PASTELL_MASTER_SECRET est requis pour le mock Pastell. " +
                            "Doit etre identique a la valeur configuree cote sejour-backend.");
        }
        this.masterSecret = masterSecret.getBytes(StandardCharsets.UTF_8);
        log.info("MockCredentialsProvider initialise (verification rotation quotidienne UTC active).");
        log.info("Username attendu : {}", getUsername());
    }

    public String getUsername() {
        byte[] hmac = computeHmac("username");
        String hex = HexFormat.of().formatHex(hmac).substring(0, USERNAME_HEX_LENGTH);
        return "sejour-" + hex;
    }

    public String getCurrentPassword() {
        return passwordForDate(LocalDate.now(ZoneOffset.UTC));
    }

    public String getYesterdayPassword() {
        return passwordForDate(LocalDate.now(ZoneOffset.UTC).minusDays(1));
    }

    public String passwordForDate(LocalDate utcDate) {
        byte[] hmac = computeHmac("password:" + utcDate.toString());
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        return b64.substring(0, PASSWORD_LENGTH);
    }

    private byte[] computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(masterSecret, HMAC_ALGO));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible dans la JVM", e);
        }
    }
}
