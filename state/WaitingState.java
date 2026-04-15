package state;

import model.QueueEntry;

public class WaitingState implements QueueState {
    public void startService(QueueEntry context) {
        System.out.println("Starting service for: " + context.getCustomerName());
        context.setState(new InProgressState()); // [cite: 144]
    }
    public void completeService(QueueEntry context) {
        System.out.println("Error: Cannot complete service from Waiting state.");
    }
    public void cancel(QueueEntry context) {
        context.setState(new CancelledState()); // [cite: 142]
    }
}