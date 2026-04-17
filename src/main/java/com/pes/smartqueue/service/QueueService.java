package com.pes.smartqueue.service;

import com.pes.smartqueue.exception.InvalidQueueTransitionException;
import com.pes.smartqueue.model.queue.EntryType;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.queue.QueueStatus;
import com.pes.smartqueue.pattern.strategy.QueueOrderingStrategy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class QueueService {
    private final AtomicLong idSequence = new AtomicLong(1L);
    private final Map<Long, QueueEntry> entries = new ConcurrentHashMap<>();
    private final Map<String, QueueOrderingStrategy> strategyMap = new ConcurrentHashMap<>();
    private QueueOrderingStrategy activeStrategy;

    public QueueService(List<QueueOrderingStrategy> strategies) {
        for (QueueOrderingStrategy strategy : strategies) {
            strategyMap.put(strategy.key(), strategy);
        }
        this.activeStrategy = strategyMap.get("APPOINTMENT_PRIORITY");
        if (this.activeStrategy == null && !strategies.isEmpty()) {
            this.activeStrategy = strategies.get(0);
        }
    }

    public synchronized QueueEntry addEntry(String customerName, EntryType type) {
        long id = idSequence.getAndIncrement();
        QueueEntry entry = new QueueEntry(id, customerName, type);
        entries.put(id, entry);
        return entry;
    }

    public synchronized List<QueueEntry> getOrderedQueue() {
        List<QueueEntry> activeEntries = entries.values().stream()
            .filter(entry -> entry.getStatus() == QueueStatus.WAITING || entry.getStatus() == QueueStatus.IN_PROGRESS)
            .toList();
        if (activeStrategy == null) {
            return activeEntries.stream().sorted(Comparator.comparing(QueueEntry::getArrivalTime)).toList();
        }
        return activeStrategy.orderQueue(activeEntries);
    }

    public synchronized List<QueueEntry> getAllEntries() {
        return new ArrayList<>(entries.values()).stream()
            .sorted(Comparator.comparing(QueueEntry::getArrivalTime))
            .toList();
    }

    public synchronized QueueEntry startNext() {
        QueueEntry next = getOrderedQueue().stream()
            .filter(entry -> entry.getStatus() == QueueStatus.WAITING)
            .findFirst()
            .orElseThrow(() -> new InvalidQueueTransitionException("No WAITING entries available"));
        next.startService();
        return next;
    }

    public synchronized void startEntry(long id) {
        QueueEntry entry = requireEntry(id);
        entry.startService();
    }

    public synchronized void completeEntry(long id) {
        QueueEntry entry = requireEntry(id);
        entry.completeService();
    }

    public synchronized void cancelEntry(long id) {
        QueueEntry entry = requireEntry(id);
        entry.cancel();
    }

    public synchronized String getActiveStrategyKey() {
        return activeStrategy != null ? activeStrategy.key() : "NONE";
    }

    public synchronized List<String> getAvailableStrategyKeys() {
        return strategyMap.keySet().stream().sorted().toList();
    }

    public synchronized void setActiveStrategy(String key) {
        QueueOrderingStrategy selected = strategyMap.get(key);
        if (selected == null) {
            throw new IllegalArgumentException("Unknown strategy: " + key);
        }
        this.activeStrategy = selected;
    }

    private QueueEntry requireEntry(long id) {
        QueueEntry entry = entries.get(id);
        if (Objects.isNull(entry)) {
            throw new IllegalArgumentException("Queue entry not found: " + id);
        }
        return entry;
    }
}
