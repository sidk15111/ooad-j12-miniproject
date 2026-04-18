package com.pes.smartqueue.model.appointment;

import com.pes.smartqueue.pattern.state.appointment.AppointmentState;
import com.pes.smartqueue.pattern.state.appointment.CancelledAppointmentState;
import com.pes.smartqueue.pattern.state.appointment.CheckedInAppointmentState;
import com.pes.smartqueue.pattern.state.appointment.ConfirmedAppointmentState;
import com.pes.smartqueue.pattern.state.appointment.CreatedAppointmentState;
import com.pes.smartqueue.pattern.state.appointment.ExpiredAppointmentState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private LocalDateTime slotTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @Transient
    private AppointmentState currentState;

    protected Appointment() {
    }

    public Appointment(String customerName, LocalDateTime slotTime) {
        this.customerName = customerName;
        this.slotTime = slotTime;
        this.status = AppointmentStatus.CREATED;
        hydrateState();
    }

    public void confirm() {
        hydrateState();
        currentState.confirm(this);
    }

    public void checkIn() {
        hydrateState();
        currentState.checkIn(this);
    }

    public void cancel() {
        hydrateState();
        currentState.cancel(this);
    }

    public void expire() {
        hydrateState();
        currentState.expire(this);
    }

    public void transitionTo(AppointmentState nextState, AppointmentStatus nextStatus) {
        this.currentState = nextState;
        this.status = nextStatus;
    }

    @PostLoad
    public void hydrateState() {
        if (status == null || status == AppointmentStatus.CREATED) {
            currentState = new CreatedAppointmentState();
            status = AppointmentStatus.CREATED;
            return;
        }
        switch (status) {
            case CONFIRMED -> currentState = new ConfirmedAppointmentState();
            case CHECKED_IN -> currentState = new CheckedInAppointmentState();
            case CANCELLED -> currentState = new CancelledAppointmentState();
            case EXPIRED -> currentState = new ExpiredAppointmentState();
            case CREATED -> currentState = new CreatedAppointmentState();
            default -> throw new IllegalStateException("Unhandled appointment state: " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDateTime getSlotTime() {
        return slotTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getCurrentStateName() {
        hydrateState();
        return currentState.name();
    }
}
