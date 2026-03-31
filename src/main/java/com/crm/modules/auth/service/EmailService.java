package com.crm.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi d'emails.
 * Utilisé pour notifier le propriétaire après la décision du SuperAdmin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envoie un email de confirmation de validation de compte.
     * Appelé par le SuperAdmin après DEV-21 (valider).
     */
    @Async
    public void envoyerEmailValidation(String destinataire, String nomEntreprise) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(destinataire);
            message.setSubject("✅ Votre compte CRM a été validé — " + nomEntreprise);
            message.setText(
                    "Bonjour,\n\n" +
                    "Nous avons le plaisir de vous informer que votre compte entreprise \"" +
                    nomEntreprise + "\" a été validé avec succès.\n\n" +
                    "Vous pouvez dès maintenant vous connecter à l'application mobile CRM.\n\n" +
                    "Cordialement,\nL'équipe CRM"
            );
            mailSender.send(message);
            log.info("Email de validation envoyé à {}", destinataire);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de validation à {} : {}", destinataire, e.getMessage());
        }
    }

    /**
     * Envoie un email de refus de compte.
     * Appelé par le SuperAdmin après DEV-21 (refuser).
     */
    @Async
    public void envoyerEmailRefus(String destinataire, String nomEntreprise, String motif) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(destinataire);
            message.setSubject("❌ Votre demande de compte CRM — " + nomEntreprise);
            message.setText(
                    "Bonjour,\n\n" +
                    "Nous vous informons que votre demande de création de compte pour l'entreprise \"" +
                    nomEntreprise + "\" n'a pas pu être acceptée.\n\n" +
                    "Motif : " + (motif != null ? motif : "Non précisé") + "\n\n" +
                    "Pour toute question, veuillez nous contacter.\n\n" +
                    "Cordialement,\nL'équipe CRM"
            );
            mailSender.send(message);
            log.info("Email de refus envoyé à {}", destinataire);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de refus à {} : {}", destinataire, e.getMessage());
        }
    }

    /**
     * Envoie un email de bienvenue après inscription.
     * Informe le propriétaire que son compte est en attente de validation.
     */
    @Async
    public void envoyerEmailBienvenue(String destinataire, String nomProprietaire, String nomEntreprise) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(destinataire);
            message.setSubject("Bienvenue sur CRM — Votre demande est en cours d'examen");
            message.setText(
                    "Bonjour " + nomProprietaire + ",\n\n" +
                    "Votre demande de création de compte pour l'entreprise \"" + nomEntreprise +
                    "\" a bien été reçue.\n\n" +
                    "Votre compte est actuellement en attente de validation par notre équipe.\n" +
                    "Vous recevrez un email dès qu'une décision sera prise.\n\n" +
                    "Cordialement,\nL'équipe CRM"
            );
            mailSender.send(message);
            log.info("Email de bienvenue envoyé à {}", destinataire);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de bienvenue à {} : {}", destinataire, e.getMessage());
        }
    }
}
