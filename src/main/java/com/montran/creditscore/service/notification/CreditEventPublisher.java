package com.montran.creditscore.service.notification;

import com.montran.creditscore.domain.model.User;
import com.montran.creditscore.domain.port.outbound.NotificationSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer-pattern publisher that implements {@link NotificationSender}.
 * Dispatches credit events asynchronously to all registered listeners so that
 * slow delivery channels (email, SMS) never block the scoring thread.
 * Uses {@link CopyOnWriteArrayList} to allow safe listener registration during event dispatch.
 */
public class CreditEventPublisher implements NotificationSender {

    private final List<CreditEventListener> listeners = new CopyOnWriteArrayList<>();

    /** Registers a listener to receive future credit events. */
    public void subscribe(CreditEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** Removes a previously registered listener. */
    public void unsubscribe(CreditEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public void sendNotification(User user, String message) {
        if (user == null || message == null || message.trim().isEmpty()) {
            return;
        }

        for (CreditEventListener listener : listeners) {
            CompletableFuture.runAsync(() -> listener.onCreditEvent(user, message))
                    .exceptionally(ex -> {
                        System.err.println("Critical failure dispatching notification to listener: "
                                + listener.getClass().getSimpleName() + ". Reason: " + ex.getMessage());
                        return null;
                    });
        }
    }
}