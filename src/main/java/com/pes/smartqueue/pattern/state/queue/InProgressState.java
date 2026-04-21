package com.pes.smartqueue.pattern.state.queue;

import com.pes.smartqueue.exception.InvalidQueueTransitionException;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.queue.QueueStatus;

public class InProgressState implements QueueState {
    @Override
    public void startService(QueueEntry context) {
        throw new InvalidQueueTransitionException("Service is already IN_PROGRESS");
    }

    @Override
    public void completeService(QueueEntry context) {
        context.transitionTo(new CompletedState(), QueueStatus.COMPLETED);
    }

    @Override
    public void markNoShow(QueueEntry context) {
        context.transitionTo(new NoShowState(), QueueStatus.NO_SHOW);
    }

    @Override
    public void cancel(QueueEntry context) {
        context.transitionTo(new CancelledState(), QueueStatus.CANCELLED);
    }

    @Override
    public void releaseToWaiting(QueueEntry context) {
        context.transitionTo(new WaitingState(), QueueStatus.WAITING);
    }

    @Override
    public String name() {
        return "IN_PROGRESS";
    }
}
