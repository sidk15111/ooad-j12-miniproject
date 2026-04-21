package com.pes.smartqueue.pattern.state.appointment;

import com.pes.smartqueue.exception.InvalidAppointmentTransitionException;
import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;

public class CreatedAppointmentState implements AppointmentState {
    @Override
    public void confirm(Appointment context) {
        context.transitionTo(new ConfirmedAppointmentState(), AppointmentStatus.CONFIRMED);
    }

    @Override
    public void reschedule(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot reschedule before confirmation");
    }

    @Override
    public void checkIn(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot check in before confirmation");
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
        return "CREATED";
    }
}
