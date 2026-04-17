package com.pes.smartqueue.model.appointment;

import com.pes.smartqueue.pattern.state.appointment.AppointmentState;
import com.pes.smartqueue.pattern.state.appointment.CreatedAppointmentState;

import java.time.LocalDateTime;

public class Appointment {
    private final long id;
    private final String customerName;
    private final LocalDateTime slotTime;
    private AppointmentStatus status;
    private AppointmentState currentState;

    public Appointment(long id, String customerName, LocalDateTime slotTime) {
        this.id = id;
        this.customerName = customerName;
        this.slotTime = slotTime;
        this.status = AppointmentStatus.CREATED;
        this.currentState = new CreatedAppointmentState();
    }

    public void confirm() {
        currentState.confirm(this);
    }

    public void checkIn() {
        currentState.checkIn(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }

    public void expire() {
        currentState.expire(this);
    }

    public void transitionTo(AppointmentState nextState, AppointmentStatus nextStatus) {
        this.currentState = nextState;
        this.status = nextStatus;
    }

    public long getId() {
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
        return currentState.name();
    }
}
