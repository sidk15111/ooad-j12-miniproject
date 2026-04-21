package com.pes.smartqueue.pattern.state.queue;

import com.pes.smartqueue.exception.InvalidQueueTransitionException;
import com.pes.smartqueue.model.queue.QueueEntry;

public class CancelledState implements QueueState {
    @Override
    public void startService(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot start service from CANCELLED state");
    }

    @Override
    public void completeService(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot complete service from CANCELLED state");
    }

    @Override
    public void markNoShow(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot mark CANCELLED entry as NO_SHOW");
    }

    @Override
    public void cancel(QueueEntry context) {
        throw new InvalidQueueTransitionException("Entry is already CANCELLED");
    }

    @Override
    public void releaseToWaiting(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot release CANCELLED entry back to WAITING");
    }

    @Override
    public String name() {
        return "CANCELLED";
    }
}
