package service;

import model.QueueEntry;
import strategy.QueueOrderingStrategy;
import strategy.PriorityAppointmentStrategy;
import java.util.ArrayList;
import java.util.List;

public class QueueService {
    private List<QueueEntry> activeQueue = new ArrayList<>();
    private QueueOrderingStrategy strategy = new PriorityAppointmentStrategy();

    public void addPatient(QueueEntry entry) {
        activeQueue.add(entry);
    }

    public List<QueueEntry> getSortedQueue() {
        return strategy.orderQueue(activeQueue);
    }
    
    public void processNext() {
        List<QueueEntry> sorted = getSortedQueue();
        if (!sorted.isEmpty()) {
            sorted.get(0).startService();
        }
    }
}