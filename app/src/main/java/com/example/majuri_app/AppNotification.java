package com.example.majuri_app;

public class AppNotification {
    private final long id;
    private final String title;
    private final String message;
    private final String timeLabel;
    private final boolean unread;

    public AppNotification(long id, String title, String message, String timeLabel, boolean unread) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timeLabel = timeLabel;
        this.unread = unread;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public boolean isUnread() {
        return unread;
    }
}
