package dev.meguru.ui.notifications;

import dev.meguru.Meguru;
import dev.meguru.module.impl.render.NotificationsMod;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    @Getter
    @Setter
    private static float toggleTime = 2;

    @Getter
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    public static void post(NotificationType type, String title, String description) {
        post(new Notification(type, title, description));
    }

    public static void post(NotificationType type, String title, String description, float time) {
        post(new Notification(type, title, description, time));
    }

    private static void post(Notification notification) {
        if (Meguru.INSTANCE.isEnabled(NotificationsMod.class)) {
            notifications.add(notification);
        }
    }

}
