package com.crm.config;

import com.crm.modules.utilisateur.entity.SuperAdmin;
import com.crm.modules.utilisateur.repository.UtilisateurRepository;
import com.crm.shared.enums.RoleUtilisateur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initialise un SuperAdmin par défaut au démarrage si aucun n'existe en base.
 * Les credentials sont lus depuis les variables d'environnement.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.email:admin@easysalescrm.com}")
    private String defaultAdminEmail;

    @Value("${admin.default.password}")
    private String defaultAdminPassword;

    @Value("${admin.default.nom:Super}")
    private String defaultAdminNom;

    @Value("${admin.default.prenom:Admin}")
    private String defaultAdminPrenom;

    @Override
    public void run(String... args) {
        if (!utilisateurRepository.existsByEmail(defaultAdminEmail)) {
            SuperAdmin admin = new SuperAdmin();
            admin.setNom(defaultAdminNom);
            admin.setPrenom(defaultAdminPrenom);
            admin.setEmail(defaultAdminEmail);
            admin.setMotDePasseHash(passwordEncoder.encode(defaultAdminPassword));
            admin.setRole(RoleUtilisateur.ROLE_SUPER_ADMIN);

            utilisateurRepository.save(admin);
            log.warn("✅ SuperAdmin par défaut créé : {}", defaultAdminEmail);
            log.warn("⚠️  Changez ce mot de passe immédiatement en production !");
        } else {
            log.info("SuperAdmin déjà présent en base — initialisation ignorée.");
        }
    }
}