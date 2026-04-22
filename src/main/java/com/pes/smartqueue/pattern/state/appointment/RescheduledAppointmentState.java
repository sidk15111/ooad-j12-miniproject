package com.pes.smartqueue.pattern.state.appointment;

import com.pes.smartqueue.exception.InvalidAppointmentTransitionException;
import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;

public class RescheduledAppointmentState implements AppointmentState {
    @Override
    public void confirm(Appointment context) {
        context.transitionTo(new ConfirmedAppointmentState(), AppointmentStatus.CONFIRMED);
    }

    @Override
    public void reschedule(Appointment context) {
        throw new InvalidAppointmentTransitionException("Appointment already rescheduled");
    }

    @Override
    public void checkIn(Appointment context) {
        throw new InvalidAppointmentTransitionException("Cannot check in rescheduled appointment before confirmation");
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
        throw new InvalidAppointmentTransitionException("Cannot expire rescheduled appointment before confirmation");
    }

    @Override
    public String name() {
        return "RESCHEDULED";
    }
}