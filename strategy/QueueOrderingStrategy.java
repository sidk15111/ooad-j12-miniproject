package strategy;

import model.QueueEntry;
import java.util.List;

public interface QueueOrderingStrategy {
    List<QueueEntry> orderQueue(List<QueueEntry> queue);
}