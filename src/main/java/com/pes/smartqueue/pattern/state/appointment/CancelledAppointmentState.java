package com.pes.smartqueue.pattern.state.appointment;

import com.pes.smartqueue.exception.InvalidAppointmentTransitionException;
import com.pes.smartqueue.model.appointment.Appointment;

public class CancelledAppointmentState implements AppointmentState {
    @Override
    public void confirm(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot confirm cancelled appointment");
    }

    @Override
    public void checkIn(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot check in cancelled appointment");
    }

    @Override
    public void cancel(Appointment context) {
        throw new InvalidAppointmentTransitionException("Appointment already cancelled");
    }

    @Override
    public void expire(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot expire cancelled appointment");
    }

    @Override
    public String name() {
        return "CANCELLED";
    }
}
