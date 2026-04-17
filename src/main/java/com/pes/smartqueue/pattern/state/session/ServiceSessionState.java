package com.pes.smartqueue.pattern.state.session;

import com.pes.smartqueue.model.session.ServiceSession;

public interface ServiceSessionState {
    void activate(ServiceSession context);

    void pause(ServiceSession context);

    void resume(ServiceSession context);

    void complete(ServiceSession context);

    String name();
}
