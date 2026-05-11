package com.example.springhotel.component;

import com.example.springhotel.entity.Privilege;
import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.PrivilegeRepository;
import com.example.springhotel.repository.RoleRepository;
import com.example.springhotel.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Initialise les donnees de base au demarrage de l'application.
 * <p>
 * <b>Evolution Lot 6 :</b> ajout du compte demo public {@code demo@springhotel.fr}
 * pour la mise en ligne. Ce compte est creee uniquement s'il n'existe pas, ce qui
 * garde le runtime idempotent : on peut redemarrer en toute securite, et un admin
 * qui modifierait le mot de passe en base ne se le ferait pas ecraser.
 * <p>
 * <b>Pourquoi seeder le compte demo ici plutot que dans une migration SQL ?</b>
 *   <ul>
 *     <li>BCrypt est natif a Spring Security : on a deja le PasswordEncoder injecte,
 *         pas besoin de calculer le hash a la main et de le coller en SQL.</li>
 *     <li>Coherence avec le compte admin {@code test@test.com} qui est deja
 *         seede ici depuis le Lot 0.</li>
 *     <li>Idempotence triviale : {@code findByEmail().isEmpty()} suffit.</li>
 *   </ul>
 */
@Component
public class SetupDataLoader implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SetupDataLoader.class);

    boolean alreadySetup = false;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {

        if (alreadySetup) return;

        // Privileges
        Privilege readPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE");
        Privilege writePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE");

        // Roles USER, ADMIN, EMPLOYE
        List<Privilege> adminPrivileges = Arrays.asList(readPrivilege, writePrivilege);
        List<Privilege> employePrivileges = Arrays.asList(readPrivilege, writePrivilege);

        createRoleIfNotFound("ROLE_ADMIN", adminPrivileges);
        createRoleIfNotFound("ROLE_EMPLOYE", employePrivileges);
        createRoleIfNotFound("ROLE_USER", List.of(readPrivilege));

        // Admin par defaut (a remplacer en prod)
        if (userRepository.findByEmail("test@test.com").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN");
            Users users = new Users();
            users.setFirstName("Test");
            users.setLastName("Test");
            users.setPassword(passwordEncoder.encode("test123"));
            users.setEmail("test@test.com");
            users.setRoles(Arrays.asList(adminRole));
            users.setEnabled(true);
            userRepository.save(users);
            log.info("SetupDataLoader : compte admin test@test.com cree.");
        }

        // Lot 6 : compte demo public pour la mise en ligne portfolio
        if (userRepository.findByEmail("demo@springhotel.fr").isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER");
            Users demo = new Users();
            demo.setFirstName("Demo");
            demo.setLastName("SpringHotel");
            demo.setPassword(passwordEncoder.encode("Malik971*"));
            demo.setEmail("demo@springhotel.fr");
            demo.setRoles(Arrays.asList(userRole));
            demo.setEnabled(true);
            userRepository.save(demo);
            log.info("SetupDataLoader : compte demo demo@springhotel.fr cree.");
        }

        alreadySetup = true;
    }

    @Transactional
    Privilege createPrivilegeIfNotFound(String name) {
        Privilege privilege = privilegeRepository.findByName(name);
        if (privilege == null) {
            privilege = new Privilege(name);
            privilegeRepository.save(privilege);
        }
        return privilege;
    }

    @Transactional
    Role createRoleIfNotFound(String name, Collection<Privilege> privileges) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name);
            role.setPrivileges(privileges);
            roleRepository.save(role);
        }
        return role;
    }
}
