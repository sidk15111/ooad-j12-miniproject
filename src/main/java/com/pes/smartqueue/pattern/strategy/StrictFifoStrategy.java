package com.pes.smartqueue.pattern.strategy;

import com.pes.smartqueue.model.queue.QueueEntry;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class StrictFifoStrategy implements QueueOrderingStrategy {
    @Override
    public List<QueueEntry> orderQueue(List<QueueEntry> queue) {
        return queue.stream()
            .sorted(Comparator.comparing(QueueEntry::getArrivalTime))
            .toList();
    }

    @Override
    public String key() {
        return "STRICT_FIFO";
    }
}
