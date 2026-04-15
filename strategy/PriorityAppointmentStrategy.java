package strategy;

import model.EntryType;
import model.QueueEntry;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PriorityAppointmentStrategy implements QueueOrderingStrategy {
    @Override
    public List<QueueEntry> orderQueue(List<QueueEntry> queue) {
        return queue.stream()
            .sorted(Comparator
                .comparing((QueueEntry e) -> e.getType() == EntryType.WALK_IN) 
                .thenComparing(QueueEntry::getArrivalTime))
            .collect(Collectors.toList());
    }
}