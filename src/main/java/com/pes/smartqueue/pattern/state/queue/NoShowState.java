package com.pes.smartqueue.pattern.state.queue;

import com.pes.smartqueue.exception.InvalidQueueTransitionException;
import com.pes.smartqueue.model.queue.QueueEntry;

public class NoShowState implements QueueState {
    @Override
    public void startService(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot start service from NO_SHOW state");
    }

    @Override
    public void completeService(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot complete service from NO_SHOW state");
    }

    @Override
    public void markNoShow(QueueEntry context) {
        throw new InvalidQueueTransitionException("Entry is already NO_SHOW");
    }

    @Override
    public void cancel(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot cancel from NO_SHOW state");
    }

    @Override
    public void releaseToWaiting(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot release NO_SHOW entry back to WAITING");
    }

    @Override
    public String name() {
        return "NO_SHOW";
    }
}