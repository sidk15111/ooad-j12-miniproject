package com.pes.smartqueue.repository;

import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.queue.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    List<QueueEntry> findAllByOrderByArrivalTimeAsc();

    List<QueueEntry> findByStatusInOrderByArrivalTimeAsc(Collection<QueueStatus> statuses);

    List<QueueEntry> findByStatusOrderByArrivalTimeAsc(QueueStatus status);

    long countByStatus(QueueStatus status);

    List<QueueEntry> findByArrivalTimeBetween(LocalDateTime start, LocalDateTime end);

    boolean existsBySourceAppointmentId(Long sourceAppointmentId);
}
