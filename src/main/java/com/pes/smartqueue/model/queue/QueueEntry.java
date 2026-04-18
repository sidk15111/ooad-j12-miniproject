package com.pes.smartqueue.model.queue;

import com.pes.smartqueue.pattern.state.queue.CancelledState;
import com.pes.smartqueue.pattern.state.queue.CompletedState;
import com.pes.smartqueue.pattern.state.queue.InProgressState;
import com.pes.smartqueue.pattern.state.queue.QueueState;
import com.pes.smartqueue.pattern.state.queue.WaitingState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_entries")
public class QueueEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    @Enumerated(EnumType.STRING)
    private EntryType type;

    private Long sourceAppointmentId;
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    private QueueStatus status;

    @Transient
    private QueueState currentState;

    protected QueueEntry() {
    }

    public QueueEntry(String customerName, EntryType type) {
        this(customerName, type, null);
    }

    public QueueEntry(String customerName, EntryType type, Long sourceAppointmentId) {
        this.customerName = customerName;
        this.type = type;
        this.sourceAppointmentId = sourceAppointmentId;
        this.arrivalTime = LocalDateTime.now();
        this.status = QueueStatus.WAITING;
        hydrateState();
    }

    public void startService() {
        hydrateState();
        currentState.startService(this);
    }

    public void completeService() {
        hydrateState();
        currentState.completeService(this);
    }

    public void cancel() {
        hydrateState();
        currentState.cancel(this);
    }

    public void transitionTo(QueueState nextState, QueueStatus nextStatus) {
        this.currentState = nextState;
        this.status = nextStatus;
    }

    @PostLoad
    public void hydrateState() {
        if (status == null || status == QueueStatus.WAITING) {
            currentState = new WaitingState();
            status = QueueStatus.WAITING;
            return;
        }
        switch (status) {
            case IN_PROGRESS -> currentState = new InProgressState();
            case COMPLETED -> currentState = new CompletedState();
            case CANCELLED -> currentState = new CancelledState();
            case WAITING -> currentState = new WaitingState();
            default -> throw new IllegalStateException("Unhandled queue state: " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public EntryType getType() {
        return type;
    }

    public Long getSourceAppointmentId() {
        return sourceAppointmentId;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public String getCurrentStateName() {
        hydrateState();
        return currentState.name();
    }
}
