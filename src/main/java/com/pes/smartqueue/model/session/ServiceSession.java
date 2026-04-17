package com.pes.smartqueue.model.session;

import com.pes.smartqueue.pattern.state.session.IdleServiceSessionState;
import com.pes.smartqueue.pattern.state.session.ServiceSessionState;

public class ServiceSession {
    private final long id;
    private final String staffUsername;
    private ServiceSessionStatus status;
    private ServiceSessionState currentState;

    public ServiceSession(long id, String staffUsername) {
        this.id = id;
        this.staffUsername = staffUsername;
        this.status = ServiceSessionStatus.IDLE;
        this.currentState = new IdleServiceSessionState();
    }

    public void activate() {
        currentState.activate(this);
    }

    public void pause() {
        currentState.pause(this);
    }

    public void resume() {
        currentState.resume(this);
    }

    public void complete() {
        currentState.complete(this);
    }

    public void transitionTo(ServiceSessionState nextState, ServiceSessionStatus nextStatus) {
        this.currentState = nextState;
        this.status = nextStatus;
    }

    public long getId() {
        return id;
    }

    public String getStaffUsername() {
        return staffUsername;
    }

    public ServiceSessionStatus getStatus() {
        return status;
    }

    public String getCurrentStateName() {
        return currentState.name();
    }
}
