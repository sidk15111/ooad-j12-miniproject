package com.pes.smartqueue.pattern.state.queue;

import com.pes.smartqueue.exception.InvalidQueueTransitionException;
import com.pes.smartqueue.model.queue.QueueEntry;

public class CompletedState implements QueueState {
    @Override
    public void startService(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot start service from COMPLETED state");
    }

    @Override
    public void completeService(QueueEntry context) {
        throw new InvalidQueueTransitionException("Service is already COMPLETED");
    }

    @Override
    public void markNoShow(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot mark COMPLETED entry as NO_SHOW");
    }

    @Override
    public void cancel(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot cancel from COMPLETED state");
    }

    @Override
    public void releaseToWaiting(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot release COMPLETED entry back to WAITING");
    }

    @Override
    public String name() {
        return "COMPLETED";
    }
}
