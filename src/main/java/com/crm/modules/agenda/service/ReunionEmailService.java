package com.crm.modules.agenda.service;

import com.crm.modules.agenda.dto.ReunionParticipantDto;
import com.crm.modules.agenda.dto.ReunionResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service d'envoi des invitations email pour les réunions CRM.
 *
 * <p>Chaque invitation contient :</p>
 * <ul>
 *   <li>Un email HTML avec les détails de la réunion</li>
 *   <li>Un fichier {@code .ics} (iCalendar) en pièce jointe —
 *       compatible Google Calendar, Outlook, Apple Calendar</li>
 *   <li>Le lien de réunion en ligne si disponible (Jitsi, Google Meet…)</li>
 * </ul>
 *
 * <p>L'envoi est {@code @Async} — il ne bloque pas la création de la réunion.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReunionEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Easy Sales CRM}")
    private String appName;

    // ─────────────────────────────────────────────────────────────────────────
    //  ENVOI INVITATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie une invitation de réunion à tous les participants ayant un email.
     *
     * <p>Exécuté de façon asynchrone pour ne pas bloquer la réponse API.
     * Si l'envoi échoue, une erreur est loggée mais la réunion reste créée.</p>
     *
     * @param reunion      DTO de la réunion créée
     * @param participants liste des participants à notifier
     */
    @Async
    public void envoyerInvitations(ReunionResponse reunion,
                                   List<ReunionParticipantDto> participants) {
        participants.stream()
                .filter(p -> p.getEmail() != null && !p.getEmail().isBlank())
                .forEach(p -> envoyerAUnParticipant(reunion, p));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie l'invitation à un participant spécifique.
     *
     * @param reunion     détails de la réunion
     * @param participant participant destinataire
     */
    private void envoyerAUnParticipant(ReunionResponse reunion,
                                       ReunionParticipantDto participant) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(participant.getEmail());
            helper.setSubject("Invitation : " + reunion.getTitre());
            helper.setText(construireCorpsHtml(reunion, participant), true);

            // Pièce jointe ICS — compatible tous les clients calendrier
            String ics = construireIcs(reunion);
            helper.addAttachment("invitation.ics",
                    () -> new java.io.ByteArrayInputStream(ics.getBytes()),
                    "text/calendar; method=REQUEST");

            mailSender.send(message);
            log.info("[REUNION EMAIL] Invitation envoyée à {} pour la réunion id={}",
                    participant.getEmail(), reunion.getId());

        } catch (MessagingException e) {
            log.warn("[REUNION EMAIL] Échec envoi à {} : {}", participant.getEmail(), e.getMessage());
        }
    }

    /**
     * Construit le corps HTML de l'invitation.
     *
     * @param reunion     détails de la réunion
     * @param participant destinataire
     * @return HTML de l'email
     */
    private String construireCorpsHtml(ReunionResponse reunion,
                                       ReunionParticipantDto participant) {
        LocalDateTime dateHeure = LocalDateTime.parse(reunion.getDateHeure());
        String dateFormatee = dateHeure.format(
                DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm",
                        java.util.Locale.FRENCH));

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>")
                .append("<div style='background:#1E3A8A;padding:24px;border-radius:8px 8px 0 0;'>")
                .append("<h1 style='color:white;margin:0;font-size:20px;'>📅 Invitation à une réunion</h1>")
                .append("</div>")
                .append("<div style='background:#f8fafc;padding:24px;border-radius:0 0 8px 8px;'>")
                .append("<p>Bonjour ").append(participant.getNom()).append(",</p>")
                .append("<p>Vous êtes invité(e) à la réunion suivante :</p>")
                .append("<div style='background:white;border-radius:8px;padding:16px;border:1px solid #e2e8f0;'>")
                .append("<h2 style='color:#1E3A8A;margin:0 0 12px 0;'>").append(reunion.getTitre()).append("</h2>")
                .append("<p>📅 <strong>Date :</strong> ").append(dateFormatee).append("</p>")
                .append("<p>⏱ <strong>Durée :</strong> ").append(formatDuree(reunion.getDureeMinutes())).append("</p>");

        if (reunion.getLieu() != null && !reunion.getLieu().isBlank()) {
            sb.append("<p>📍 <strong>Lieu :</strong> ").append(reunion.getLieu()).append("</p>");
        }

        if (reunion.getLienReunion() != null && !reunion.getLienReunion().isBlank()) {
            sb.append("<p>🔗 <strong>Lien :</strong> <a href='")
                    .append(reunion.getLienReunion()).append("'>")
                    .append(reunion.getLienReunion()).append("</a></p>");
        }

        if (reunion.getNotes() != null && !reunion.getNotes().isBlank()) {
            sb.append("<p>📝 <strong>Notes :</strong> ").append(reunion.getNotes()).append("</p>");
        }

        sb.append("</div>")
                .append("<p style='color:#64748b;font-size:12px;margin-top:16px;'>")
                .append("Un fichier .ics est joint à cet email. Importez-le dans votre calendrier (Google Calendar, Outlook, Apple Calendar).</p>")
                .append("<p style='color:#64748b;font-size:12px;'>Envoyé via ").append(appName).append("</p>")
                .append("</div></div>");

        return sb.toString();
    }

    /**
     * Génère le contenu ICS (iCalendar) de la réunion.
     * Compatible Google Calendar, Outlook, Apple Calendar.
     *
     * @param reunion détails de la réunion
     * @return contenu ICS sous forme de chaîne
     */
    private String construireIcs(ReunionResponse reunion) {
        LocalDateTime dateHeure   = LocalDateTime.parse(reunion.getDateHeure());
        LocalDateTime dateFin     = dateHeure.plusMinutes(reunion.getDureeMinutes());
        ZoneId        zoneTunisie = ZoneId.of("Africa/Tunis");

        DateTimeFormatter icsFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String dtStart = dateHeure.atZone(zoneTunisie).withZoneSameInstant(ZoneOffset.UTC).format(icsFormat);
        String dtEnd   = dateFin.atZone(zoneTunisie).withZoneSameInstant(ZoneOffset.UTC).format(icsFormat);

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//Easy Sales CRM//Easy Sales CRM//FR\r\n")
                .append("CALSCALE:GREGORIAN\r\n")
                .append("METHOD:REQUEST\r\n")
                .append("BEGIN:VEVENT\r\n")
                .append("UID:").append(UUID.randomUUID()).append("@easysalescrm\r\n")
                .append("DTSTART:").append(dtStart).append("\r\n")
                .append("DTEND:").append(dtEnd).append("\r\n")
                .append("SUMMARY:").append(reunion.getTitre()).append("\r\n")
                .append("ORGANIZER:MAILTO:").append(fromEmail).append("\r\n");

        if (reunion.getLieu() != null && !reunion.getLieu().isBlank()) {
            ics.append("LOCATION:").append(reunion.getLieu()).append("\r\n");
        }

        if (reunion.getLienReunion() != null && !reunion.getLienReunion().isBlank()) {
            ics.append("URL:").append(reunion.getLienReunion()).append("\r\n");
        }

        if (reunion.getNotes() != null && !reunion.getNotes().isBlank()) {
            ics.append("DESCRIPTION:").append(reunion.getNotes().replace("\n", "\\n")).append("\r\n");
        }

        ics.append("STATUS:CONFIRMED\r\n")
                .append("END:VEVENT\r\n")
                .append("END:VCALENDAR\r\n");

        return ics.toString();
    }

    /**
     * Formate une durée en minutes en texte lisible.
     *
     * @param minutes durée en minutes
     * @return texte formaté (ex: "1h30", "45 min")
     */
    private String formatDuree(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        return m > 0 ? h + "h" + String.format("%02d", m) : h + "h";
    }
}