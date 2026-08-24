package com.campushub.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RealtimeEventListener {

    private final RealtimeWebSocketHandler webSocketHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChange(RealtimeChangeEvent change) {
        if (change.userId() == null) {
            webSocketHandler.broadcast(change.event());
        } else {
            webSocketHandler.sendToUser(change.userId(), change.event());
        }
    }
}
