package model;

import state.QueueState;
import state.WaitingState;
import java.time.LocalDateTime;

public class QueueEntry {
    private String customerName;
    private EntryType type;
    private LocalDateTime arrivalTime;
    private QueueState currentState;

    public QueueEntry(String customerName, EntryType type) {
        this.customerName = customerName;
        this.type = type;
        this.arrivalTime = LocalDateTime.now();
        this.currentState = new WaitingState(); // Initial state [cite: 143]
    }

    public void startService() { currentState.startService(this); }
    public void completeService() { currentState.completeService(this); }
    public void cancel() { currentState.cancel(this); }

    public void setState(QueueState state) { this.currentState = state; }
    public QueueState getCurrentState() { return currentState; }
    public EntryType getType() { return type; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public String getCustomerName() { return customerName; }
}