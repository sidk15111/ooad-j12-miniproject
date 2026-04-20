package com.pes.smartqueue.pattern.state.queue;

import com.pes.smartqueue.model.queue.QueueEntry;

public interface QueueState {
    void startService(QueueEntry context);

    void completeService(QueueEntry context);

    void cancel(QueueEntry context);

    void releaseToWaiting(QueueEntry context);

    String name();
}
