package com.pes.smartqueue.pattern.state.queue;

import com.pes.smartqueue.exception.InvalidQueueTransitionException;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.queue.QueueStatus;

public class WaitingState implements QueueState {
    @Override
    public void startService(QueueEntry context) {
        context.transitionTo(new InProgressState(), QueueStatus.IN_PROGRESS);
    }

    @Override
    public void completeService(QueueEntry context) {
        throw new InvalidQueueTransitionException("Cannot complete from WAITING state");
    }

    @Override
    public void cancel(QueueEntry context) {
        context.transitionTo(new CancelledState(), QueueStatus.CANCELLED);
    }

    @Override
    public void releaseToWaiting(QueueEntry context) {
        throw new InvalidQueueTransitionException("Entry is already WAITING");
    }

    @Override
    public String name() {
        return "WAITING";
    }
}
