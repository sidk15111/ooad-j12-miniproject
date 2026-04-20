package com.pes.smartqueue.pattern.state.session;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;

public class ActiveSessionState implements ServiceSessionState {
    @Override
    public void activate(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Session already ACTIVE");
    }

    @Override
    public void pause(ServiceSession context) {
        context.transitionTo(new PausedSessionState(), ServiceSessionStatus.PAUSED);
    }

    @Override
    public void resume(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Session is already ACTIVE");
    }

    @Override
    public void complete(ServiceSession context) {
        context.transitionTo(new CompletedSessionState(), ServiceSessionStatus.COMPLETED);
    }

    @Override
    public String name() {
        return "ACTIVE";
    }
}
