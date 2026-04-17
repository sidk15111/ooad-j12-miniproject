package com.pes.smartqueue.pattern.state.appointment;

import com.pes.smartqueue.exception.InvalidAppointmentTransitionException;
import com.pes.smartqueue.model.appointment.Appointment;

public class CheckedInAppointmentState implements AppointmentState {
    @Override
    public void confirm(Appointment context) {
        throw new InvalidAppointmentTransitionException("Appointment already checked in");
    }

    @Override
    public void checkIn(Appointment context) {
        throw new InvalidAppointmentTransitionException("Appointment already checked in");
    }

    @Override
    public void cancel(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot cancel checked-in appointment");
    }

    @Override
    public void expire(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot expire checked-in appointment");
    }

    @Override
    public String name() {
        return "CHECKED_IN";
    }
}
