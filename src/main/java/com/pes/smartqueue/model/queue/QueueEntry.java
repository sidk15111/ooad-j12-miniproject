package com.pes.smartqueue.model.queue;

import com.pes.smartqueue.pattern.state.queue.QueueState;
import com.pes.smartqueue.pattern.state.queue.WaitingState;

import java.time.LocalDateTime;

public class QueueEntry {
    private final long id;
    private final String customerName;
    private final EntryType type;
    private final LocalDateTime arrivalTime;
    private QueueStatus status;
    private QueueState currentState;

    public QueueEntry(long id, String customerName, EntryType type) {
        this.id = id;
        this.customerName = customerName;
        this.type = type;
        this.arrivalTime = LocalDateTime.now();
        this.status = QueueStatus.WAITING;
        this.currentState = new WaitingState();
    }

    public void startService() {
        currentState.startService(this);
    }

    public void completeService() {
        currentState.completeService(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }

    public void transitionTo(QueueState nextState, QueueStatus nextStatus) {
        this.currentState = nextState;
        this.status = nextStatus;
    }

    public long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public EntryType getType() {
        return type;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public String getCurrentStateName() {
        return currentState.name();
    }
}
