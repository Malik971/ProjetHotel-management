package com.example.springhotel.security.jwt;

import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.RoleRepository;
import com.example.springhotel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'integration du filtre JWT avec une chaine Spring Security complete.
 * <p>
 * On verifie le comportement bout-en-bout sur GET /api/me :
 *   un, sans token, on doit recevoir 401 avec un JSON propre,
 *   deux, avec un token invalide, 401,
 *   trois, avec un token valide d'un utilisateur existant, 200 et le bon DTO.
 * <p>
 * On utilise le profil test qui pointe sur H2 plutot que sur la base de prod.
 * Le JWT_SECRET de test est defini dans application-test.properties.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationFilterOldJWTIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private Users testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Le SetupDataLoader cree ROLE_USER au demarrage. On s'appuie dessus.
        Role userRole = roleRepository.findByName("ROLE_USER");

        // Idempotence : on cree l'user s'il n'existe pas, sinon on le recupere
        Optional<Users> existing = userRepository.findByEmail("jwt-test@example.com");
        if (existing.isPresent()) {
            testUser = existing.get();
        } else {
            Users u = new Users();
            u.setEmail("jwt-test@example.com");
            u.setFirstName("Jwt");
            u.setLastName("Test");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setEnabled(true);
            u.setRoles(List.of(userRole));
            testUser = userRepository.save(u);
        }
    }

    @Test
    @DisplayName("GET /api/me sans header renvoie 401 et un JSON propre")
    void me_sans_token_renvoie_401() throws Exception {
        mockMvc.perform(get("/api/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/me avec un token corrompu renvoie 401")
    void me_avec_token_corrompu_renvoie_401() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer ceci-n-est-pas-un-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/me avec un token valide renvoie 200 et le bon DTO")
    void me_avec_token_valide_renvoie_200() throws Exception {
        String token = jwtService.generateToken(
                testUser.getEmail(),
                testUser.getId(),
                List.of("ROLE_USER")
        );

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jwt-test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jwt"))
                .andExpect(jsonPath("$.lastName").value("Test"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/hotels reste public (permitAll)")
    void hotels_public_meme_sans_token() throws Exception {
        mockMvc.perform(get("/api/hotels").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}