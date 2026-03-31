package com.crm.config;

import com.crm.modules.utilisateur.entity.SuperAdmin;
import com.crm.modules.utilisateur.repository.UtilisateurRepository;
import com.crm.shared.enums.RoleUtilisateur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initialise un SuperAdmin par défaut au démarrage de l'application
 * si aucun n'existe en base.
 *
 * Credentials par défaut (à changer impérativement en production) :
 *   email    : admin@crm.com
 *   password : Admin@1234
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_ADMIN_EMAIL    = "admin@crm.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@1234";

    @Override
    public void run(String... args) {
        if (!utilisateurRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            SuperAdmin admin = new SuperAdmin();
            admin.setNom("Super");
            admin.setPrenom("Admin");
            admin.setEmail(DEFAULT_ADMIN_EMAIL);
            admin.setMotDePasseHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            admin.setRole(RoleUtilisateur.ROLE_SUPER_ADMIN);

            utilisateurRepository.save(admin);
            log.warn("✅ SuperAdmin par défaut créé : {} / {}",
                    DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
            log.warn("⚠️  Changez ce mot de passe immédiatement en production !");
        } else {
            log.info("SuperAdmin déjà présent en base — initialisation ignorée.");
        }
    }
}
