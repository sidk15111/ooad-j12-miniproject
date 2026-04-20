package com.pes.smartqueue.pattern.state.session;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.session.ServiceSession;

public class CompletedSessionState implements ServiceSessionState {
    @Override
    public void activate(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot start a COMPLETED session");
    }

    @Override
    public void pause(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot pause a COMPLETED session");
    }

    @Override
    public void resume(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot resume a COMPLETED session");
    }

    @Override
    public void complete(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Session already COMPLETED");
    }

    @Override
    public String name() {
        return "COMPLETED";
    }
}
