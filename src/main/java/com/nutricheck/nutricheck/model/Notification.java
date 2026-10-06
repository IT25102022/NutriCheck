package com.nutricheck.nutricheck.model;

import java.time.LocalDateTime;

/**
 * Temporary In-Memory Notification Model.
 * Notifications are ephemeral, non-persistent, and immutable after creation.
 * They are NOT stored in the database.
 */
public class Notification {

    private final Long id;
    private final User recipient;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final LocalDateTime createdAt;

    private volatile boolean isRead = false;
    private volatile LocalDateTime readAt;

    public Notification(Long id, User recipient, String title, String message, NotificationType type, LocalDateTime createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.title = title;
        this.message = message;
        this.type = type != null ? type : NotificationType.SYSTEM;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.isRead = false;
    }

    public Notification(User recipient, String title, String message, NotificationType type) {
        this(null, recipient, title, message, type, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
