package com.crm.modules.agenda.service;

import com.crm.modules.agenda.dto.ReunionParticipantDto;
import com.crm.modules.agenda.dto.ReunionRequest;
import com.crm.modules.agenda.dto.ReunionResponse;
import com.crm.modules.agenda.entity.Reunion;
import com.crm.modules.agenda.entity.ReunionParticipant;
import com.crm.modules.agenda.mapper.ReunionMapper;
import com.crm.modules.agenda.repository.ReunionRepository;
import com.crm.modules.client.entity.Client;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.enums.StatutReunion;
import com.crm.shared.enums.TypeParticipant;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des réunions client.
 *
 * <p><b>Double booking :</b> vérifié à chaque création et modification.
 * Une exception {@link BusinessException} est levée si un conflit horaire existe.</p>
 *
 * <p><b>Jitsi Meet :</b> un lien est généré automatiquement si {@code enLigne = true}
 * et qu'aucun lien n'est fourni. Le champ {@code lienReunion} est générique —
 * pour migrer vers Google Meet, il suffit de passer l'URL Google Meet.</p>
 *
 * <p><b>Email :</b> envoyé de façon asynchrone aux participants ayant un email,
 * avec une pièce jointe .ics (calendrier).</p>
 *
 * @author Riahi Dorsaf
 * @see IReunionService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReunionService implements IReunionService {

    private final ReunionRepository reunionRepository;
    private final ClientRepository clientRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final ReunionMapper reunionMapper;
    private final ReunionEmailService reunionEmailService;

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<ReunionResponse> lister(Long proprietaireId) {
        return reunionRepository
                .findByProprietaireIdOrderByDateHeureAsc(proprietaireId)
                .stream()
                .map(this::enrichir)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTE PAR SEMAINE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<ReunionResponse> listerSemaine(Long proprietaireId,
                                               String debutSemaine,
                                               String finSemaine) {
        LocalDateTime debut = LocalDate.parse(debutSemaine).atStartOfDay();
        LocalDateTime fin = LocalDate.parse(finSemaine).atTime(LocalTime.MAX);

        return reunionRepository
                .findByProprietaireIdAndDateHeureBetweenOrderByDateHeureAsc(
                        proprietaireId, debut, fin)
                .stream()
                .map(this::enrichir)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTE PAR CLIENT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<ReunionResponse> listerParClient(Long clientId, Long proprietaireId) {
        verifierAccesClient(clientId, proprietaireId);
        return reunionRepository
                .findByClientIdAndProprietaireIdOrderByDateHeureAsc(clientId, proprietaireId)
                .stream()
                .map(this::enrichir)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DÉTAIL
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public ReunionResponse obtenir(Long id, Long proprietaireId) {
        return enrichir(charger(id, proprietaireId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CRÉATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ReunionResponse creer(ReunionRequest req, Long proprietaireId) {
        Client client = verifierAccesClient(req.getClientId(), proprietaireId);

        ProprietaireEntreprise proprietaire = proprietaireRepository
                .findById(proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable"));

        // Vérification double booking
        verifierConflitHoraire(proprietaireId, req.getDateHeure(),
                req.getDureeMinutes(), null);

        // Génération lien Jitsi si réunion en ligne sans lien fourni
        String lienReunion = req.getLienReunion();
        if (Boolean.TRUE.equals(req.getEnLigne()) &&
                (lienReunion == null || lienReunion.isBlank())) {
            lienReunion = genererLienJitsi();
        }

        // Conversion participants
        List<ReunionParticipant> participants = req.getParticipants().stream()
                .map(reunionMapper::toParticipant)
                .collect(Collectors.toList());

        Reunion reunion = Reunion.builder()
                .titre(req.getTitre())
                .dateHeure(req.getDateHeure())
                .dureeMinutes(req.getDureeMinutes())
                .lieu(req.getLieu())
                .notes(req.getNotes())
                .enLigne(Boolean.TRUE.equals(req.getEnLigne()))
                .lienReunion(lienReunion)
                .statut(StatutReunion.PLANIFIEE)
                .rappelsMinutes(req.getRappelsMinutes())
                .participants(participants)
                .client(client)
                .proprietaire(proprietaire)
                .build();

        reunion = reunionRepository.save(reunion);
        log.info("[REUNION] Créée — id={} client={} date={}",
                reunion.getId(), client.getNomAffichage(), reunion.getDateHeure());

        ReunionResponse response = enrichir(reunion);

        // Envoi invitations email (asynchrone).
        // Automatique si la réunion est en ligne — le client principal et les
        // participants (contacts, externes) ayant un email reçoivent
        // l'invitation calendrier (.ics). Sinon, sur demande explicite.
        if (reunion.isEnLigne() || Boolean.TRUE.equals(req.getEnvoyerInvitation())) {
            List<ReunionParticipantDto> destinataires =
                    construireDestinataires(client, req.getParticipants());
            if (!destinataires.isEmpty()) {
                reunionEmailService.envoyerInvitations(response, destinataires);
            }
        }

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MODIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ReunionResponse modifier(Long id, ReunionRequest req, Long proprietaireId) {
        Reunion reunion = charger(id, proprietaireId);

        if (!StatutReunion.PLANIFIEE.equals(reunion.getStatut())) {
            throw new BusinessException("Seules les réunions planifiées peuvent être modifiées.");
        }

        Client client = verifierAccesClient(req.getClientId(), proprietaireId);

        // Vérification double booking (exclut la réunion courante)
        verifierConflitHoraire(proprietaireId, req.getDateHeure(),
                req.getDureeMinutes(), id);

        // Génération lien Jitsi si besoin
        String lienReunion = req.getLienReunion();
        if (Boolean.TRUE.equals(req.getEnLigne()) &&
                (lienReunion == null || lienReunion.isBlank())) {
            lienReunion = reunion.getLienReunion() != null
                    ? reunion.getLienReunion()
                    : genererLienJitsi();
        }

        List<ReunionParticipant> participants = req.getParticipants().stream()
                .map(reunionMapper::toParticipant)
                .collect(Collectors.toList());

        reunion.setTitre(req.getTitre());
        reunion.setDateHeure(req.getDateHeure());
        reunion.setDureeMinutes(req.getDureeMinutes());
        reunion.setLieu(req.getLieu());
        reunion.setNotes(req.getNotes());
        reunion.setEnLigne(Boolean.TRUE.equals(req.getEnLigne()));
        reunion.setLienReunion(lienReunion);
        reunion.setRappelsMinutes(req.getRappelsMinutes());
        reunion.setParticipants(participants);
        reunion.setClient(client);

        reunion = reunionRepository.save(reunion);
        log.info("[REUNION] Modifiée — id={}", id);

        return enrichir(reunion);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TERMINER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void terminer(Long id, Long proprietaireId) {
        Reunion reunion = charger(id, proprietaireId);
        if (StatutReunion.TERMINEE.equals(reunion.getStatut())) {
            throw new BusinessException("Cette réunion est déjà terminée.");
        }
        if (StatutReunion.ANNULEE.equals(reunion.getStatut())) {
            throw new BusinessException("Impossible de terminer une réunion annulée.");
        }
        reunion.setStatut(StatutReunion.TERMINEE);
        reunionRepository.save(reunion);
        log.info("[REUNION] Terminée — id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANNULER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void annuler(Long id, Long proprietaireId) {
        Reunion reunion = charger(id, proprietaireId);
        if (StatutReunion.ANNULEE.equals(reunion.getStatut())) {
            throw new BusinessException("Cette réunion est déjà annulée.");
        }
        if (StatutReunion.TERMINEE.equals(reunion.getStatut())) {
            throw new BusinessException("Impossible d'annuler une réunion déjà terminée.");
        }
        reunion.setStatut(StatutReunion.ANNULEE);
        reunionRepository.save(reunion);
        log.info("[REUNION] Annulée — id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPPRESSION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void supprimer(Long id, Long proprietaireId) {
        Reunion reunion = charger(id, proprietaireId);
        reunionRepository.delete(reunion);
        log.info("[REUNION] Supprimée — id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Charge une réunion en vérifiant l'appartenance au propriétaire.
     *
     * @param id             identifiant de la réunion
     * @param proprietaireId identifiant du propriétaire connecté
     * @return la réunion
     * @throws ResourceNotFoundException si introuvable
     */
    private Reunion charger(Long id, Long proprietaireId) {
        return reunionRepository
                .findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Réunion introuvable"));
    }

    /**
     * Vérifie que le client appartient au propriétaire connecté.
     *
     * @param clientId       identifiant du client
     * @param proprietaireId identifiant du propriétaire
     * @return le client
     * @throws ResourceNotFoundException si introuvable
     */
    private Client verifierAccesClient(Long clientId, Long proprietaireId) {
        return clientRepository
                .findByIdAndProprietaireIdAndIsDeletedFalse(clientId, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
    }

    /**
     * Vérifie l'absence de conflit horaire (double booking).
     *
     * <p>Un conflit existe si deux réunions se chevauchent dans le temps.
     * Seules les réunions au statut {@link StatutReunion#PLANIFIEE} sont prises en compte.</p>
     *
     * @param proprietaireId identifiant du propriétaire
     * @param dateHeure      date/heure de début de la nouvelle réunion
     * @param dureeMinutes   durée en minutes
     * @param excludeId      identifiant à exclure (en mode modification, null sinon)
     * @throws BusinessException si un conflit horaire est détecté
     */
    private void verifierConflitHoraire(Long proprietaireId,
                                        LocalDateTime dateHeure,
                                        int dureeMinutes,
                                        Long excludeId) {
        LocalDateTime finNouvelle = dateHeure.plusMinutes(dureeMinutes);

        List<Reunion> planifiees = reunionRepository
                .findByProprietaireIdAndStatutOrderByDateHeureAsc(
                        proprietaireId, StatutReunion.PLANIFIEE);

        for (Reunion r : planifiees) {
            if (excludeId != null && r.getId().equals(excludeId)) continue;

            LocalDateTime rFin = r.getDateHeure().plusMinutes(r.getDureeMinutes());
            boolean chevauchement = dateHeure.isBefore(rFin) && finNouvelle.isAfter(r.getDateHeure());

            if (chevauchement) {
                throw new BusinessException(
                        "Conflit horaire : une réunion \"" + r.getTitre()
                                + "\" est déjà planifiée de "
                                + r.getDateHeure().toLocalTime()
                                + " à " + rFin.toLocalTime() + ".");
            }
        }
    }

    /**
     * Construit la liste des destinataires de l'invitation calendrier.
     *
     * <p>Inclut le client principal (email de sa fiche) en plus des
     * participants saisis. Le client n'est ajouté que s'il a un email
     * et qu'aucun participant ne porte déjà cet email
     * (dédoublonnage insensible à la casse).</p>
     *
     * @param client       client principal de la réunion
     * @param participants participants saisis dans le formulaire
     * @return liste des destinataires de l'invitation
     */
    private List<ReunionParticipantDto> construireDestinataires(
            Client client, List<ReunionParticipantDto> participants) {

        List<ReunionParticipantDto> destinataires = new ArrayList<>(participants);

        String emailClient = client.getEmail();
        if (emailClient != null && !emailClient.isBlank()) {
            boolean dejaPresent = participants.stream()
                    .anyMatch(p -> emailClient.equalsIgnoreCase(p.getEmail()));
            if (!dejaPresent) {
                destinataires.add(ReunionParticipantDto.builder()
                        .nom(client.getNomAffichage())
                        .email(emailClient)
                        .type(TypeParticipant.CLIENT)
                        .build());
            }
        }
        return destinataires;
    }

    /**
     * Génère un lien Jitsi Meet unique pour la réunion.
     *
     * <p>Pour migrer vers Google Meet ou Teams, il suffit de passer
     * l'URL correspondante dans {@code lienReunion} — aucun changement
     * de code nécessaire.</p>
     *
     * @return URL Jitsi Meet unique
     */
    private String genererLienJitsi() {
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return "https://meet.jit.si/EasySalesCRM-" + code;
    }

    /**
     * Enrichit le {@link ReunionResponse} avec les données calculées.
     *
     * @param reunion entité à enrichir
     * @return DTO complet
     */
    private ReunionResponse enrichir(Reunion reunion) {
        ReunionResponse response = reunionMapper.toResponse(reunion);
        response.setClientNom(reunion.getClient().getNomAffichage());

        List<ReunionParticipantDto> participantDtos = reunion.getParticipants()
                .stream()
                .map(reunionMapper::toParticipantDto)
                .toList();
        response.setParticipants(participantDtos);
        response.setDateRelative(dateRelative(reunion.getDateHeure()));
        return response;
    }

    /**
     * Calcule une date relative lisible.
     *
     * @param dateHeure date/heure de la réunion
     * @return ex: "dans 2 h", "demain", "il y a 3 j"
     */
    private String dateRelative(LocalDateTime dateHeure) {
        if (dateHeure == null) return "";
        LocalDateTime now = LocalDateTime.now();
        Duration d = Duration.between(now, dateHeure);
        long minutes = d.toMinutes();

        if (minutes < 0) {
            long abs = Math.abs(minutes);
            if (abs < 60) return "il y a " + abs + " min";
            long h = Math.abs(d.toHours());
            if (h < 24) return "il y a " + h + " h";
            return "il y a " + Math.abs(d.toDays()) + " j";
        }
        if (minutes < 60) return "dans " + minutes + " min";
        long h = d.toHours();
        if (h < 24) return "dans " + h + " h";
        long j = d.toDays();
        if (j == 1) return "demain";
        return "dans " + j + " j";
    }
}