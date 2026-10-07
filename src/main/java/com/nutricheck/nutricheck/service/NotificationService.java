package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.Notification;
import com.nutricheck.nutricheck.model.NotificationType;
import com.nutricheck.nutricheck.model.Role;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for temporary in-memory notifications.
 * All notification records exist in application memory only and are never saved to MySQL.
 * Notifications are strictly immutable after creation; only read status can be updated.
 */
@Service
public class NotificationService {

    private final TemporaryNotificationStore notificationStore;
    private final UserRepository userRepository;

    public NotificationService(TemporaryNotificationStore notificationStore, UserRepository userRepository) {
        this.notificationStore = notificationStore;
        this.userRepository = userRepository;
    }

    public Notification createNotification(User recipient, String title, String message, NotificationType type) {
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient is required.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification title cannot be blank.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message cannot be blank.");
        }

        return notificationStore.addNotification(recipient, title.trim(), message.trim(), type);
    }

    public List<Notification> createNotificationForRole(Role role, String title, String message, NotificationType type) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required for role-based broadcast.");
        }
        if (title == null || title.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Title and message are required.");
        }

        List<User> targetUsers = userRepository.findByRole(role);
        List<Notification> created = new ArrayList<>();
        for (User user : targetUsers) {
            created.add(notificationStore.addNotification(user, title.trim(), message.trim(), type));
        }
        return created;
    }

    public List<Notification> createNotificationForAll(String title, String message, NotificationType type) {
        if (title == null || title.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Title and message are required.");
        }

        List<User> allUsers = userRepository.findAll();
        List<Notification> created = new ArrayList<>();
        for (User user : allUsers) {
            created.add(notificationStore.addNotification(user, title.trim(), message.trim(), type));
        }
        return created;
    }

    public List<Notification> getNotificationsForUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }
        return notificationStore.findByRecipient(user);
    }

    public long getUnreadCountForUser(User user) {
        if (user == null) {
            return 0;
        }
        return notificationStore.countUnreadByRecipient(user);
    }

    public Optional<Notification> getNotificationById(Long id) {
        return notificationStore.findById(id);
    }

    public Notification markAsRead(Long notificationId, User currentUser) {
        Notification notification = notificationStore.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));

        boolean isRecipient = notification.getRecipient().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isRecipient && !isAdmin) {
            throw new SecurityException("Unauthorized: You can only modify your own notifications.");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        return notification;
    }

    public void markAllAsRead(User currentUser) {
        if (currentUser == null) {
            return;
        }
        List<Notification> notifications = notificationStore.findByRecipient(currentUser);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : notifications) {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(now);
            }
        }
    }

    public List<Notification> getAllNotifications() {
        return notificationStore.findAll();
    }

    public long countTotalNotifications() {
        return notificationStore.countTotal();
    }

    public long countUnreadNotifications() {
        return notificationStore.countUnreadTotal();
    }

    public void clearNotificationsForUser(User user) {
        notificationStore.removeByUser(user);
    }

    public void clearAll() {
        notificationStore.clearAll();
    }
}
