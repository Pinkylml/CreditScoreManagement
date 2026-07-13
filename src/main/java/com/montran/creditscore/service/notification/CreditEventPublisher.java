package com.montran.creditscore.service.notification;

import com.montran.creditscore.domain.model.User;
import com.montran.creditscore.domain.port.outbound.NotificationSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @docs Subject implementation for the Observer Pattern managing notification
 *       subscriptions and broadcasting events.
 *       <p>
 *       <b>Design Justification:</b> Implements the Outbound Port
 *       'NotificationSender'. Utilizes asynchronous execution threads via
 *       CompletableFuture to ensure the main mathematical evaluation threads
 *       are never blocked by high-latency external delivery systems.
 *       </p>
 */
public class CreditEventPublisher implements NotificationSender {

    /**
     * @docs Thread-safe collection holding active observer subscriptions.
     *       Utilizes CopyOnWriteArrayList to prevent
     *       ConcurrentModificationExceptions during active iteration.
     */
    private final List<CreditEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * @docs Subscribes a new observer to receive future credit events.
     * @param listener The non-null event listener to append to the broadcast list.
     */
    public void subscribe(CreditEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * @docs Unsubscribes an existing observer to halt their reception of credit
     *       events.
     * @param listener The event listener to remove from the broadcast list.
     */
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