package com.pes.smartqueue.pattern.state.appointment;

import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;
import com.pes.smartqueue.exception.InvalidAppointmentTransitionException;

public class ConfirmedAppointmentState implements AppointmentState {
    @Override
    public void confirm(Appointment context) {
    }

    @Override
    public void checkIn(Appointment context) {
        context.transitionTo(new CheckedInAppointmentState(), AppointmentStatus.CHECKED_IN);
    }

    @Override
    public void complete(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot complete before check-in");
    }

    @Override
    public void cancel(Appointment context) {
        context.transitionTo(new CancelledAppointmentState(), AppointmentStatus.CANCELLED);
    }

    @Override
    public void expire(Appointment context) {
        context.transitionTo(new ExpiredAppointmentState(), AppointmentStatus.EXPIRED);
    }

    @Override
    public String name() {
        return "CONFIRMED";
    }
}
