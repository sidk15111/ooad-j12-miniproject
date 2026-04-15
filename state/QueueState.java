package state;

import model.QueueEntry;

public interface QueueState {
    void startService(QueueEntry context);
    void completeService(QueueEntry context);
    void cancel(QueueEntry context);
}