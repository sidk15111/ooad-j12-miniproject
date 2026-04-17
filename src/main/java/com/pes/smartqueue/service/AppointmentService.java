package com.pes.smartqueue.service;

import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AppointmentService {
    private final AtomicLong idSequence = new AtomicLong(1L);
    private final Map<Long, Appointment> appointments = new ConcurrentHashMap<>();

    public synchronized Appointment create(String customerName, LocalDateTime slotTime) {
        long id = idSequence.getAndIncrement();
        Appointment appointment = new Appointment(id, customerName, slotTime);
        appointments.put(id, appointment);
        return appointment;
    }

    public synchronized void confirm(long id) {
        require(id).confirm();
    }

    public synchronized void checkIn(long id) {
        require(id).checkIn();
    }

    public synchronized void cancel(long id) {
        require(id).cancel();
    }

    public synchronized void expirePastDue() {
        LocalDateTime now = LocalDateTime.now();
        appointments.values().stream()
            .filter(appointment -> appointment.getStatus() != AppointmentStatus.CHECKED_IN)
            .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
            .filter(appointment -> appointment.getStatus() != AppointmentStatus.EXPIRED)
            .filter(appointment -> appointment.getSlotTime().isBefore(now.minusMinutes(30)))
            .forEach(Appointment::expire);
    }

    public synchronized List<Appointment> list() {
        return appointments.values().stream()
            .sorted(Comparator.comparing(Appointment::getSlotTime))
            .toList();
    }

    private Appointment require(long id) {
        Appointment appointment = appointments.get(id);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found: " + id);
        }
        return appointment;
    }
}
