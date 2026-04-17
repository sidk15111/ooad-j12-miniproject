package com.pes.smartqueue.pattern.state.session;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;

public class IdleServiceSessionState implements ServiceSessionState {
    @Override
    public void activate(ServiceSession context) {
        context.transitionTo(new ActiveServiceSessionState(), ServiceSessionStatus.ACTIVE);
    }

    @Override
    public void pause(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot pause from IDLE");
    }

    @Override
    public void resume(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot resume from IDLE");
    }

    @Override
    public void complete(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot complete from IDLE");
    }

    @Override
    public String name() {
        return "IDLE";
    }
}
