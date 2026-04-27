package com.example.pastellmock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration du handshake Pastell mock.
 *
 * Verifie deux choses :
 *   1. {@code /api/version.php} est accessible SANS credentials (anonyme)
 *      et renvoie le payload attendu.
 *   2. La regle "tout le reste de /api/** est protege" fonctionne :
 *      un autre endpoint sous /api/ renvoie 401 sans credentials,
 *      et fonctionne avec les bons credentials.
 *
 * Note : on utilise un endpoint inexistant (/api/_protected_probe) pour le
 * second test. Spring Security s'execute AVANT le routage Spring MVC, donc
 * la requete est rejetee avec 401 avant meme que MVC essaie de trouver le
 * controller. Avec credentials valides, on attend un 404 (l'auth a passe,
 * mais la route n'existe pas) -- c'est exactement ce qu'on veut prouver :
 * l'auth passe, le reste du systeme prend le relais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "pastell.mock.username=test-user",
        "pastell.mock.password=test-pwd"
})
class VersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void version_endpoint_is_accessible_anonymously() throws Exception {
        mockMvc.perform(get("/api/version.php"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pastell"))
                .andExpect(jsonPath("$.version").value("3.0-mock"))
                .andExpect(jsonPath("$.edition").value("mock"));
    }

    @Test
    void other_api_endpoints_require_authentication() throws Exception {
        // Sans credentials : 401, peu importe que la route existe ou non
        mockMvc.perform(get("/api/_protected_probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void other_api_endpoints_pass_auth_with_valid_credentials() throws Exception {
        // Avec credentials valides : Spring Security laisse passer.
        // La route n'existe pas, donc on attend 404 -- ce qui prouve
        // exactement que l'authentification a reussi.
        mockMvc.perform(get("/api/_protected_probe")
                        .with(httpBasic("test-user", "test-pwd")))
                .andExpect(status().isNotFound());
    }

    @Test
    void other_api_endpoints_reject_invalid_credentials() throws Exception {
        mockMvc.perform(get("/api/_protected_probe")
                        .with(httpBasic("test-user", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }
}