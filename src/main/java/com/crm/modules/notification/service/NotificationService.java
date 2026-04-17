package com.crm.modules.notification.service;

import com.crm.modules.notification.dto.NotificationResponse;
import com.crm.modules.notification.entity.Notification;
import com.crm.modules.notification.entity.TypeNotification;
import com.crm.modules.notification.repository.NotificationRepository;
import com.crm.shared.exception.ResourceNotFoundException;
import com.crm.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des notifications Super Admin.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listerNotifications(int page, int size) {
        Page<Notification> pageResult = notificationRepository
                .findAllByOrderByDateCreationDesc(PageRequest.of(page, size));

        long nbNonLues = notificationRepository.countByLueFalse();

        Page<NotificationResponse> mapped = pageResult.map(n ->
                toResponse(n, nbNonLues));

        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public long compterNonLues() {
        return notificationRepository.countByLueFalse();
    }

    @Transactional
    public NotificationResponse marquerCommeLue(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification introuvable avec l'id : " + id));

        notification.setLue(true);
        notificationRepository.save(notification);
        log.info("[NOTIFICATION] Notification {} marquée comme lue", id);

        return toResponse(notification, notificationRepository.countByLueFalse());
    }

    @Transactional
    public void marquerToutesCommeLues() {
        notificationRepository.findAll().forEach(n -> n.setLue(true));
        notificationRepository.flush();
        log.info("[NOTIFICATION] Toutes les notifications marquées comme lues");
    }

    /**
     * Crée une notification interne — appelé par les autres services.
     */
    @Transactional
    public void creerNotification(
            String titre, String message,
            TypeNotification type, String lienRessource) {

        Notification notification = Notification.builder()
                .titre(titre)
                .message(message)
                .type(type)
                .lienRessource(lienRessource)
                .build();

        notificationRepository.save(notification);
        log.info("[NOTIFICATION] Nouvelle notification créée : {} ({})", titre, type);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n, long nbNonLues) {
        return NotificationResponse.builder()
                .id(n.getId())
                .titre(n.getTitre())
                .message(n.getMessage())
                .type(n.getType())
                .lue(n.isLue())
                .dateCreation(n.getDateCreation())
                .lienRessource(n.getLienRessource())
                .nbNonLues(nbNonLues)
                .build();
    }
}