package com.pes.smartqueue.pattern.state.session;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;

public class PausedSessionState implements ServiceSessionState {
    @Override
    public void activate(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot start a PAUSED session");
    }

    @Override
    public void pause(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Session is already PAUSED");
    }

    @Override
    public void resume(ServiceSession context) {
        context.transitionTo(new ActiveSessionState(), ServiceSessionStatus.ACTIVE);
    }

    @Override
    public void complete(ServiceSession context) {
        context.transitionTo(new CompletedSessionState(), ServiceSessionStatus.COMPLETED);
    }

    @Override
    public String name() {
        return "PAUSED";
    }
}
