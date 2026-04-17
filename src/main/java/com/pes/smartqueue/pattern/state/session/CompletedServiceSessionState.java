package com.pes.smartqueue.pattern.state.session;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.session.ServiceSession;

public class CompletedServiceSessionState implements ServiceSessionState {
    @Override
    public void activate(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot activate completed session");
    }

    @Override
    public void pause(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot pause completed session");
    }

    @Override
    public void resume(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot resume completed session");
    }

    @Override
    public void complete(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Session already completed");
    }

    @Override
    public String name() {
        return "COMPLETED";
    }
}
