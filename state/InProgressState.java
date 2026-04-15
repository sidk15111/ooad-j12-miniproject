package state;

import model.QueueEntry;

public class InProgressState implements QueueState {
    public void startService(QueueEntry context) {
        System.out.println("Error: Service already in progress.");
    }
    public void completeService(QueueEntry context) {
        System.out.println("Service finished for: " + context.getCustomerName());
        context.setState(new CompletedState()); // [cite: 147]
    }
    public void cancel(QueueEntry context) {
        context.setState(new CancelledState());
    }
}