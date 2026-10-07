package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.Notification;
import com.nutricheck.nutricheck.model.NotificationType;
import com.nutricheck.nutricheck.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory store for temporary server-side notifications.
 * Notifications are ephemeral, non-persistent in MySQL, and automatically bounded to prevent memory leaks.
 */
@Component
public class TemporaryNotificationStore {

    private static final int MAX_NOTIFICATIONS_PER_USER = 50;
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, Notification> notificationsById = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> userNotificationIds = new ConcurrentHashMap<>();

    public synchronized Notification addNotification(User recipient, String title, String message, NotificationType type) {
        if (recipient == null || recipient.getId() == null) {
            throw new IllegalArgumentException("Recipient with valid ID is required.");
        }
        long id = idGenerator.getAndIncrement();
        Notification notification = new Notification(id, recipient, title, message, type, LocalDateTime.now());

        notificationsById.put(id, notification);

        List<Long> idList = userNotificationIds.computeIfAbsent(recipient.getId(), k -> Collections.synchronizedList(new ArrayList<>()));
        idList.add(0, id); // Insert at beginning (most recent first)

        // Bounded capacity: Evict older notifications when limit exceeded
        if (idList.size() > MAX_NOTIFICATIONS_PER_USER) {
            synchronized (idList) {
                while (idList.size() > MAX_NOTIFICATIONS_PER_USER) {
                    Long removedId = idList.remove(idList.size() - 1);
                    if (removedId != null) {
                        notificationsById.remove(removedId);
                    }
                }
            }
        }

        return notification;
    }

    public Optional<Notification> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(notificationsById.get(id));
    }

    public List<Notification> findByRecipient(User recipient) {
        if (recipient == null || recipient.getId() == null) return Collections.emptyList();
        List<Long> ids = userNotificationIds.get(recipient.getId());
        if (ids == null) return Collections.emptyList();

        synchronized (ids) {
            return ids.stream()
                    .map(notificationsById::get)
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        }
    }

    public long countUnreadByRecipient(User recipient) {
        return findByRecipient(recipient).stream()
                .filter(n -> !n.isRead())
                .count();
    }

    public List<Notification> findAll() {
        return notificationsById.values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public long countTotal() {
        return notificationsById.size();
    }

    public long countUnreadTotal() {
        return notificationsById.values().stream()
                .filter(n -> !n.isRead())
                .count();
    }

    public void removeByUser(User user) {
        if (user == null || user.getId() == null) return;
        List<Long> ids = userNotificationIds.remove(user.getId());
        if (ids != null) {
            synchronized (ids) {
                for (Long id : ids) {
                    notificationsById.remove(id);
                }
            }
        }
    }

    public void clearAll() {
        notificationsById.clear();
        userNotificationIds.clear();
    }
}
