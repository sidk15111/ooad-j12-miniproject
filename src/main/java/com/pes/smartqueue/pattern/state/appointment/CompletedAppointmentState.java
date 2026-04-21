package com.pes.smartqueue.pattern.state.appointment;

import com.pes.smartqueue.exception.InvalidAppointmentTransitionException;
import com.pes.smartqueue.model.appointment.Appointment;

public class CompletedAppointmentState implements AppointmentState {
    @Override
    public void confirm(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot confirm completed appointment");
    }

    @Override
    public void reschedule(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot reschedule completed appointment");
    }

    @Override
    public void checkIn(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot check in completed appointment");
    }

    @Override
    public void complete(Appointment context) {
        throw new InvalidAppointmentTransitionException("Appointment already completed");
    }

    @Override
    public void cancel(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot cancel completed appointment");
    }

    @Override
    public void expire(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot expire completed appointment");
    }

    @Override
    public String name() {
        return "COMPLETED";
    }
}