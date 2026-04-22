package com.pes.smartqueue.service;

import com.pes.smartqueue.model.appointment.AppointmentStatus;
import com.pes.smartqueue.model.queue.QueueStatus;
import com.pes.smartqueue.model.session.ServiceSessionStatus;
import com.pes.smartqueue.repository.AppointmentRepository;
import com.pes.smartqueue.repository.QueueEntryRepository;
import com.pes.smartqueue.repository.ServiceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MetricsService {
    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final ServiceSessionRepository serviceSessionRepository;

    public MetricsService(AppointmentRepository appointmentRepository,
                          QueueEntryRepository queueEntryRepository,
                          ServiceSessionRepository serviceSessionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.serviceSessionRepository = serviceSessionRepository;
    }

    public long countByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    public long countQueueByStatus(QueueStatus status) {
        return queueEntryRepository.countByStatus(status);
    }

    public long countSessionByStatus(ServiceSessionStatus status) {
        return serviceSessionRepository.countByStatus(status);
    }

    public Map<String, Long> getAppointmentStatusBreakdown() {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (AppointmentStatus status : appointmentStatusesForDashboard()) {
            breakdown.put(status.name(), countByStatus(status));
        }
        return breakdown;
    }

    public Map<String, Long> getQueueStatusBreakdown() {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (QueueStatus status : QueueStatus.values()) {
            breakdown.put(status.name(), countQueueByStatus(status));
        }
        return breakdown;
    }

    private java.util.List<AppointmentStatus> appointmentStatusesForDashboard() {
        return Arrays.stream(AppointmentStatus.values())
            .filter(status -> status != AppointmentStatus.RESCHEDULED)
            .collect(Collectors.toList());
    }
}