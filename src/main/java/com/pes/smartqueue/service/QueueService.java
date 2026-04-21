package com.pes.smartqueue.service;

import com.pes.smartqueue.exception.InvalidQueueTransitionException;
import com.pes.smartqueue.model.queue.EntryType;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.queue.QueueStatus;
import com.pes.smartqueue.pattern.strategy.QueueOrderingStrategy;
import com.pes.smartqueue.repository.QueueEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class QueueService {
    private final QueueEntryRepository queueEntryRepository;
    private final Map<String, QueueOrderingStrategy> strategyMap = new ConcurrentHashMap<>();
    private QueueOrderingStrategy activeStrategy;

    public QueueService(QueueEntryRepository queueEntryRepository, List<QueueOrderingStrategy> strategies) {
        this.queueEntryRepository = queueEntryRepository;
        for (QueueOrderingStrategy strategy : strategies) {
            strategyMap.put(strategy.key(), strategy);
        }
        this.activeStrategy = strategyMap.get("APPOINTMENT_PRIORITY");
        if (this.activeStrategy == null && !strategies.isEmpty()) {
            this.activeStrategy = strategies.get(0);
        }
    }

    public QueueEntry addEntry(String customerName, EntryType type) {
        QueueEntry entry = new QueueEntry(customerName, type);
        return queueEntryRepository.save(entry);
    }

    public QueueEntry addAppointmentEntry(long appointmentId, String customerName) {
        if (queueEntryRepository.existsBySourceAppointmentId(appointmentId)) {
            throw new IllegalArgumentException("Appointment already added to queue: " + appointmentId);
        }
        QueueEntry entry = new QueueEntry(customerName, EntryType.APPOINTMENT, appointmentId);
        return queueEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<QueueEntry> getOrderedQueue() {
        List<QueueEntry> activeEntries = queueEntryRepository.findByStatusInOrderByArrivalTimeAsc(
            List.of(QueueStatus.WAITING, QueueStatus.IN_PROGRESS)
        );
        if (activeStrategy == null) {
            return activeEntries.stream().sorted(Comparator.comparing(QueueEntry::getArrivalTime)).toList();
        }
        return activeStrategy.orderQueue(activeEntries);
    }

    @Transactional(readOnly = true)
    public List<QueueEntry> getAllEntries() {
        return queueEntryRepository.findAllByOrderByArrivalTimeAsc();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized QueueEntry startNext() {
        QueueEntry next = getOrderedQueue().stream()
            .filter(entry -> entry.getStatus() == QueueStatus.WAITING)
            .findFirst()
            .orElseThrow(() -> new InvalidQueueTransitionException("No WAITING entries available"));
        next.startService();
        return queueEntryRepository.save(next);
    }

    public void requeueEntry(long id) {
        QueueEntry entry = requireEntry(id);
        entry.releaseToWaiting();
        queueEntryRepository.save(entry);
    }

    public void startEntry(long id) {
        QueueEntry entry = requireEntry(id);
        entry.startService();
        queueEntryRepository.save(entry);
    }

    public void completeEntry(long id) {
        QueueEntry entry = requireEntry(id);
        entry.completeService();
        queueEntryRepository.save(entry);
    }

    public void cancelEntry(long id) {
        QueueEntry entry = requireEntry(id);
        entry.cancel();
        queueEntryRepository.save(entry);
    }

    public void markNoShow(long id) {
        QueueEntry entry = requireEntry(id);
        entry.markNoShow();
        queueEntryRepository.save(entry);
    }

    public String getActiveStrategyKey() {
        return activeStrategy != null ? activeStrategy.key() : "NONE";
    }

    public List<String> getAvailableStrategyKeys() {
        return strategyMap.keySet().stream().sorted().toList();
    }

    public void setActiveStrategy(String key) {
        QueueOrderingStrategy selected = strategyMap.get(key);
        if (selected == null) {
            throw new IllegalArgumentException("Unknown strategy: " + key);
        }
        this.activeStrategy = selected;
    }

    @Transactional(readOnly = true)
    public boolean isAppointmentQueued(long appointmentId) {
        return queueEntryRepository.existsBySourceAppointmentId(appointmentId);
    }

    @Transactional(readOnly = true)
    public QueueEntry getById(long id) {
        return requireEntry(id);
    }

    @Transactional(readOnly = true)
    public QueueEntry findByIdOrNull(long id) {
        return queueEntryRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<QueueEntry> getInProgressEntries() {
        return queueEntryRepository.findByStatusOrderByArrivalTimeAsc(QueueStatus.IN_PROGRESS);
    }

    private QueueEntry requireEntry(long id) {
        return queueEntryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Queue entry not found: " + id));
    }
}
