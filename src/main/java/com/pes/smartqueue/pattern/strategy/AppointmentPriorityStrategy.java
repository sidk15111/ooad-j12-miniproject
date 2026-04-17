package com.pes.smartqueue.pattern.strategy;

import com.pes.smartqueue.model.queue.EntryType;
import com.pes.smartqueue.model.queue.QueueEntry;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AppointmentPriorityStrategy implements QueueOrderingStrategy {
    @Override
    public List<QueueEntry> orderQueue(List<QueueEntry> queue) {
        return queue.stream()
            .sorted(Comparator
                .comparing((QueueEntry entry) -> entry.getType() == EntryType.WALK_IN)
                .thenComparing(QueueEntry::getArrivalTime))
            .toList();
    }

    @Override
    public String key() {
        return "APPOINTMENT_PRIORITY";
    }
}
