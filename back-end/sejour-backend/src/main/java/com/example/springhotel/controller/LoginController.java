package com.example.springhotel.controller;

import com.example.springhotel.dto.LoginResponseDTO;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.UserRepository;
import com.example.springhotel.security.jwt.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Endpoint d'authentification.
 * <p>
 * Evolution Lot 0 : avant, le login renvoyait un objet UserResponse avec
 * id, email, firstName, lastName, roles. Apres, il renvoie un LoginResponseDTO
 * contenant un token JWT signe. Le contrat change pour permettre une
 * authentification stateless sur les endpoints suivants.
 * <p>
 * Le password en base est compare via BCrypt comme avant. Si le compte est
 * inconnu ou le password incorrect, on renvoie 401 avec un message generique
 * pour ne pas divulguer si un email existe en base ou non (defense contre
 * l'enumeration d'utilisateurs).
 *
 * @see com.example.springhotel.security.jwt.JwtService
 * @see LoginResponseDTO
 */
@RestController
@RequestMapping("/api/v1")
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {

        Optional<Users> userOptional = userRepository.findByEmail(loginRequest.getEmail());
        if (userOptional.isEmpty()) {
            // Reponse generique pour eviter l'enumeration d'utilisateurs
            return ResponseEntity.status(401).body("Identifiants invalides");
        }

        Users existingUser = userOptional.get();

        if (!passwordEncoder.matches(loginRequest.getPassword(), existingUser.getPassword())) {
            return ResponseEntity.status(401).body("Identifiants invalides");
        }

        List<String> roles = existingUser.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList());

        String token = jwtService.generateToken(
                existingUser.getEmail(),
                existingUser.getId(),
                roles
        );

        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                existingUser.getEmail(),
                roles
        ));
    }

    /**
     * Payload attendu en entree du login. Garde la meme structure qu'avant
     * pour ne pas casser les clients existants.
     */
    static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}