package com.pes.smartqueue.service;

import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;
import com.pes.smartqueue.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Appointment create(String customerName, LocalDateTime slotTime) {
        Appointment appointment = new Appointment(customerName, slotTime);
        return appointmentRepository.save(appointment);
    }

    public void confirm(long id) {
        Appointment appointment = require(id);
        appointment.confirm();
        appointmentRepository.save(appointment);
    }

    public void checkIn(long id) {
        Appointment appointment = require(id);
        appointment.checkIn();
        appointmentRepository.save(appointment);
    }

    public void reschedule(long id, LocalDateTime newSlotTime) {
        Appointment appointment = require(id);
        appointment.reschedule();
        appointment.setSlotTime(newSlotTime);
        appointmentRepository.save(appointment);
    }

    public void complete(long id) {
        Appointment appointment = require(id);
        appointment.complete();
        appointmentRepository.save(appointment);
    }

    public void cancel(long id) {
        Appointment appointment = require(id);
        appointment.cancel();
        appointmentRepository.save(appointment);
    }

    public void expirePastDue() {
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentStatus> activeStatuses = List.of(
            AppointmentStatus.CREATED,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.RESCHEDULED
        );
        List<Appointment> toExpire = appointmentRepository.findByStatusInAndSlotTimeBefore(activeStatuses, now.minusMinutes(30));
        for (Appointment appointment : toExpire) {
            appointment.expire();
            appointmentRepository.save(appointment);
        }
    }

    @Transactional(readOnly = true)
    public List<Appointment> list() {
        return appointmentRepository.findAllByOrderBySlotTimeAsc();
    }

    @Transactional(readOnly = true)
    public List<Appointment> listCheckedIn() {
        return appointmentRepository.findByStatusOrderBySlotTimeAsc(AppointmentStatus.CHECKED_IN);
    }

    @Transactional(readOnly = true)
    public Appointment get(long id) {
        return require(id);
    }

    private Appointment require(long id) {
        return appointmentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    }
}
