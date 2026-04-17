package com.crm.modules.notification.dto;

import com.crm.modules.notification.entity.TypeNotification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour une notification.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class NotificationResponse {

    private Long id;
    private String titre;
    private String message;
    private TypeNotification type;
    private boolean lue;
    private LocalDateTime dateCreation;
    private String lienRessource;
    private long nbNonLues;
}