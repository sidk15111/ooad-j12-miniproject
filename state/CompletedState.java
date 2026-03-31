package state;
import model.QueueEntry;

public class CompletedState implements QueueState {
    public void startService(QueueEntry context) {}
    public void completeService(QueueEntry context) {}
    public void cancel(QueueEntry context) {}
}