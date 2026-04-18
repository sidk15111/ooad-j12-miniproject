package com.pes.smartqueue.repository;

import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByOrderBySlotTimeAsc();

    List<Appointment> findByStatusOrderBySlotTimeAsc(AppointmentStatus status);

    List<Appointment> findByStatusInAndSlotTimeBefore(Collection<AppointmentStatus> statuses, LocalDateTime cutoff);
}
