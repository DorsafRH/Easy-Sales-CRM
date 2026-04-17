package com.crm.modules.notification.repository;

import com.crm.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByOrderByDateCreationDesc(Pageable pageable);

    long countByLueFalse();
}