package com.example.springhotel.service;

import com.example.springhotel.entity.Privilege;
import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.RoleRepository;
import com.example.springhotel.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // ✅ Utilisation de Optional<User>
        Optional<Users> userOptional = userRepository.findByEmail(email);

        // ✅ Vérification avec Optional
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException(
                    "Utilisateur introuvable avec l'email : " + email
            );
        }

        Users users = userOptional.get();

        return new org.springframework.security.core.userdetails.User(
                users.getEmail(),
                users.getPassword(),
                users.isEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                getAuthorities(users.getRoles())
        );
    }

    // 🔐 Rôles + privilèges (sécurisé)
    private Collection<? extends GrantedAuthority> getAuthorities(Collection<Role> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Role role : roles) {
            // ✅ ROLE_XXX
            authorities.add(new SimpleGrantedAuthority(role.getName()));

            // ✅ PRIVILEGES (null-safe)
            if (role.getPrivileges() != null) {
                for (Privilege privilege : role.getPrivileges()) {
                    authorities.add(new SimpleGrantedAuthority(privilege.getName()));
                }
            }
        }

        return authorities;
    }
}