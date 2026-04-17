package com.pes.smartqueue.pattern.strategy;

import com.pes.smartqueue.model.queue.QueueEntry;

import java.util.List;

public interface QueueOrderingStrategy {
    List<QueueEntry> orderQueue(List<QueueEntry> queue);

    String key();
}
