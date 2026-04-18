package com.pes.smartqueue.model.session;

import com.pes.smartqueue.pattern.state.session.IdleServiceSessionState;
import com.pes.smartqueue.pattern.state.session.ServiceSessionState;
import com.pes.smartqueue.pattern.state.session.ActiveServiceSessionState;
import com.pes.smartqueue.pattern.state.session.CompletedServiceSessionState;
import com.pes.smartqueue.pattern.state.session.PausedServiceSessionState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "service_sessions")
public class ServiceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String staffUsername;
    private Long activeQueueEntryId;

    @Enumerated(EnumType.STRING)
    private ServiceSessionStatus status;

    @Transient
    private ServiceSessionState currentState;

    protected ServiceSession() {
    }

    public ServiceSession(String staffUsername) {
        this.staffUsername = staffUsername;
        this.activeQueueEntryId = null;
        this.status = ServiceSessionStatus.IDLE;
        hydrateState();
    }

    public void activate() {
        hydrateState();
        currentState.activate(this);
    }

    public void pause() {
        hydrateState();
        currentState.pause(this);
    }

    public void resume() {
        hydrateState();
        currentState.resume(this);
    }

    public void complete() {
        hydrateState();
        currentState.complete(this);
    }

    public void transitionTo(ServiceSessionState nextState, ServiceSessionStatus nextStatus) {
        this.currentState = nextState;
        this.status = nextStatus;
    }

    public void assignQueueEntry(long queueEntryId) {
        this.activeQueueEntryId = queueEntryId;
    }

    public void clearQueueEntry() {
        this.activeQueueEntryId = null;
    }

    public Long getId() {
        return id;
    }

    public String getStaffUsername() {
        return staffUsername;
    }

    public ServiceSessionStatus getStatus() {
        return status;
    }

    public String getCurrentStateName() {
        hydrateState();
        return currentState.name();
    }

    public Long getActiveQueueEntryId() {
        return activeQueueEntryId;
    }

    @PostLoad
    public void hydrateState() {
        if (status == null || status == ServiceSessionStatus.IDLE) {
            currentState = new IdleServiceSessionState();
            status = ServiceSessionStatus.IDLE;
            return;
        }
        switch (status) {
            case ACTIVE -> currentState = new ActiveServiceSessionState();
            case PAUSED -> currentState = new PausedServiceSessionState();
            case COMPLETED -> currentState = new CompletedServiceSessionState();
            case IDLE -> currentState = new IdleServiceSessionState();
            default -> throw new IllegalStateException("Unhandled service session state: " + status);
        }
    }
}
