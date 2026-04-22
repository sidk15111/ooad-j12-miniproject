package com.pes.smartqueue.pattern.state.appointment;

import com.pes.smartqueue.exception.InvalidAppointmentTransitionException;
import com.pes.smartqueue.model.appointment.Appointment;

public class ExpiredAppointmentState implements AppointmentState {
    @Override
    public void confirm(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot confirm expired appointment");
    }

    @Override
    public void checkIn(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot check in expired appointment");
    }

    @Override
    public void complete(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot complete expired appointment");
    }

    @Override
    public void cancel(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot cancel expired appointment");
    }

    @Override
    public void expire(Appointment context) {
        throw new InvalidAppointmentTransitionException("Appointment already expired");
    }

    @Override
    public String name() {
        return "EXPIRED";
    }
}
