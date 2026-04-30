package com.example.springhotel.integration.pastell.e2e;

import com.example.springhotel.dto.ReservationRequestDTO;
import com.example.springhotel.entity.Chambre;
import com.example.springhotel.entity.Hotel;
import com.example.springhotel.repository.ChambreRepository;
import com.example.springhotel.repository.HotelRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Base abstraite pour les tests d'integration end-to-end de l'integration Pastell.
 *
 * Architecture WireMock : pourquoi un static initializer plutot qu'une extension JUnit ?
 *
 *   Probleme rencontre avec WireMockExtension @RegisterExtension :
 *     - L'ordre des callbacks JUnit 5 est defini par l'ordre de registration des extensions.
 *     - SpringExtension (registered via @SpringBootTest) s'execute AVANT WireMockExtension
 *       (registered via @RegisterExtension sur un champ).
 *     - SpringExtension.beforeAll() construit le contexte Spring et evalue @DynamicPropertySource.
 *     - A ce moment, WireMockExtension.beforeAll() n'a pas encore tourne, donc WireMock n'ecoute
 *       sur aucun port. Resultat : Connection refused des le premier appel HTTP.
 *
 *   Solution : static initializer.
 *     - Les blocs static d'une classe sont executes au chargement de la classe par la JVM,
 *       AVANT n'importe quel callback JUnit (garanti par la spec Java).
 *     - Quand @DynamicPropertySource est evalue, WireMock ecoute deja sur son port.
 *     - C'est exactement le pattern recommande par Spring Boot pour Testcontainers,
 *       qui souffre du meme probleme d'ordre.
 *
 * Cycle de vie :
 *   - WireMock demarre une fois par chargement de la classe (donc une fois par JVM,
 *     car les classes Java ne sont chargees qu'une seule fois).
 *   - Pas d'arret explicite : la JVM nettoie en sortie. Acceptable pour des tests.
 *   - resetAll() avant chaque test pour garantir l'isolation des stubs et compteurs.
 *
 * Partage entre sous-classes :
 *   - Le champ static est attache a PastellEndToEndTestBase, pas aux sous-classes.
 *   - Les 3 tests E2E partagent la meme instance WireMock sur le meme port.
 *   - Ce n'est pas un probleme grace au resetAll() systematique.
 */
public abstract class PastellEndToEndTestBase {

    /**
     * Serveur WireMock partage entre tous les tests E2E, sur un port aleatoire.
     */
    protected static final WireMockServer wireMock =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    /*
     * Static initializer : demarrage de WireMock garanti AVANT toute extension JUnit.
     * Spec Java : un static block est execute lors du chargement de la classe,
     * ce qui se produit avant que toute methode (callback inclus) ne soit invoquee.
     */
    static {
        wireMock.start();
    }

    /**
     * Injecte dynamiquement l'URL WireMock dans pastell.url avant la construction
     * du contexte Spring. wireMock.port() est garanti valide ici grace au static initializer.
     */
    @DynamicPropertySource
    static void registerPastellUrl(DynamicPropertyRegistry registry) {
        registry.add("pastell.url", () -> "http://localhost:" + wireMock.port());
    }

    @Autowired
    protected HotelRepository hotelRepository;

    @Autowired
    protected ChambreRepository chambreRepository;

    /** Chambre persistee pour construire des reservations valides dans les tests. */
    protected Chambre chambre;

    @BeforeEach
    void setUpBaseData() {
        // Reset des stubs WireMock pour isoler chaque test
        wireMock.resetAll();

        // Cree un hotel + chambre frais pour chaque test.
        // chambre.cascade=ALL fera disparaitre les reservations associees,
        // qui a leur tour cascadent les PastellSync (FK ON DELETE CASCADE en SQL).
        chambreRepository.deleteAll();
        hotelRepository.deleteAll();

        Hotel hotel = Hotel.builder()
                .nom("Hotel Test Montpellier")
                .ville("Montpellier")
                .adresse("1 rue du Test")
                .description("Hotel cree pour les tests E2E Pastell")
                .noteMoyenne(4.5)
                .imageUrl("https://example.com/hotel.jpg")
                .latitude(43.6108)
                .longitude(3.8767)
                .prixMoyenNuit(120.0)
                .build();
        hotel = hotelRepository.save(hotel);

        chambre = Chambre.builder()
                .nom("Chambre Test")
                .prixParNuit(new BigDecimal("100.00"))
                .capacity(2)
                .superficie(20)
                .typeLit("Lit Queen")
                .description("Chambre standard pour tests")
                .hotel(hotel)
                .build();
        chambre = chambreRepository.save(chambre);
    }

    /**
     * Construit une ReservationRequestDTO valide pour la chambre de test.
     */
    protected ReservationRequestDTO buildReservationRequest() {
        ReservationRequestDTO request = new ReservationRequestDTO();
        request.setChambreId(chambre.getId());
        request.setDateDebut(LocalDate.now().plusDays(7));
        request.setDateFin(LocalDate.now().plusDays(10));
        request.setNomClient("Jean Dupont");
        request.setEmailClient("jean.dupont@example.com");
        request.setTelephoneClient("0612345678");
        request.setNombrePersonnes(2);
        return request;
    }
}