package com.pes.smartqueue.service;

import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.queue.QueueStatus;
import com.pes.smartqueue.repository.AppointmentRepository;
import com.pes.smartqueue.repository.QueueEntryRepository;
import com.pes.smartqueue.repository.ServiceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final ServiceSessionRepository serviceSessionRepository;

    public ReportService(AppointmentRepository appointmentRepository,
                         QueueEntryRepository queueEntryRepository,
                         ServiceSessionRepository serviceSessionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.serviceSessionRepository = serviceSessionRepository;
    }

    public String generateDailyReport(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return generateReport("Daily", start, end);
    }

    public String generateWeeklyReport(LocalDate weekStart) {
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = start.plusDays(7);
        return generateReport("Weekly", start, end);
    }

    private String generateReport(String type, LocalDateTime start, LocalDateTime end) {
        List<Appointment> windowAppointments = appointmentRepository.findBySlotTimeBetween(start, end);
        List<QueueEntry> windowQueueEntries = queueEntryRepository.findByArrivalTimeBetween(start, end);

        Map<String, Long> windowAppointmentByStatus = summarizeAppointments(windowAppointments);
        Map<String, Long> windowQueueByStatus = summarizeQueue(windowQueueEntries);

        Map<String, Long> liveAppointmentByStatus = currentAppointmentSnapshot();
        Map<String, Long> liveQueueByStatus = currentQueueSnapshot();
        long totalSessions = serviceSessionRepository.count();

        StringBuilder builder = new StringBuilder();
        builder.append("SmartQueue ").append(type).append(" Report\n");
        builder.append("Window: ").append(start).append(" to ").append(end).append("\n\n");

        builder.append("Window activity (entries with slot/arrival inside window)\n\n");

        builder.append("Appointments by status\n");
        for (Map.Entry<String, Long> entry : windowAppointmentByStatus.entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        builder.append("Total appointments: ").append(windowAppointments.size()).append("\n\n");

        builder.append("Queue entries by status\n");
        for (Map.Entry<String, Long> entry : windowQueueByStatus.entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        builder.append("Total queue entries: ").append(windowQueueEntries.size()).append("\n\n");

        builder.append("Live snapshot (current system totals)\n\n");

        builder.append("Appointments by status\n");
        for (Map.Entry<String, Long> entry : liveAppointmentByStatus.entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        builder.append("Total appointments: ").append(appointmentRepository.count()).append("\n\n");

        builder.append("Queue entries by status\n");
        for (Map.Entry<String, Long> entry : liveQueueByStatus.entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        builder.append("Total queue entries: ").append(queueEntryRepository.count()).append("\n\n");

        builder.append("Total sessions: ").append(totalSessions).append("\n");
        return builder.toString();
    }

    private Map<String, Long> summarizeAppointments(List<Appointment> appointments) {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (AppointmentStatus status : appointmentStatusesForReporting()) {
            long count = appointments.stream().filter(appointment -> appointment.getStatus() == status).count();
            summary.put(status.name(), count);
        }
        return summary;
    }

    private Map<String, Long> summarizeQueue(List<QueueEntry> queueEntries) {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (QueueStatus status : QueueStatus.values()) {
            long count = queueEntries.stream().filter(entry -> entry.getStatus() == status).count();
            summary.put(status.name(), count);
        }
        return summary;
    }

    private Map<String, Long> currentAppointmentSnapshot() {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (AppointmentStatus status : appointmentStatusesForReporting()) {
            summary.put(status.name(), appointmentRepository.countByStatus(status));
        }
        return summary;
    }

    private Map<String, Long> currentQueueSnapshot() {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (QueueStatus status : QueueStatus.values()) {
            summary.put(status.name(), queueEntryRepository.countByStatus(status));
        }
        return summary;
    }

    private List<AppointmentStatus> appointmentStatusesForReporting() {
        return Arrays.stream(AppointmentStatus.values())
            .filter(status -> status != AppointmentStatus.RESCHEDULED)
            .collect(Collectors.toList());
    }
}