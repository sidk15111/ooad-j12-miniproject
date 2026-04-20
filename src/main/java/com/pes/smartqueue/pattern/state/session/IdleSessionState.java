package com.pes.smartqueue.pattern.state.session;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;

public class IdleSessionState implements ServiceSessionState {
    @Override
    public void activate(ServiceSession context) {
        context.transitionTo(new ActiveSessionState(), ServiceSessionStatus.ACTIVE);
    }

    @Override
    public void pause(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot pause an IDLE session");
    }

    @Override
    public void resume(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot resume an IDLE session");
    }

    @Override
    public void complete(ServiceSession context) {
        throw new InvalidServiceSessionTransitionException("Cannot complete an IDLE session");
    }

    @Override
    public String name() {
        return "IDLE";
    }
}
