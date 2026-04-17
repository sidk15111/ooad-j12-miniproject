package com.pes.smartqueue.pattern.state.session;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;

public class PausedServiceSessionState implements ServiceSessionState {
    @Override
    public void activate(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Use resume from PAUSED");
    }

    @Override
    public void pause(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Session already PAUSED");
    }

    @Override
    public void resume(ServiceSession context) {
        context.transitionTo(new ActiveServiceSessionState(), ServiceSessionStatus.ACTIVE);
    }

    @Override
    public void complete(ServiceSession context) {
        context.transitionTo(new CompletedServiceSessionState(), ServiceSessionStatus.COMPLETED);
    }

    @Override
    public String name() {
        return "PAUSED";
    }
}
