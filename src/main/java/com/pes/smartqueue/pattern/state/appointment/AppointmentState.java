package com.pes.smartqueue.pattern.state.appointment;

import com.pes.smartqueue.model.appointment.Appointment;

public interface AppointmentState {
    void confirm(Appointment context);

    void checkIn(Appointment context);

    void complete(Appointment context);

    void cancel(Appointment context);

    void expire(Appointment context);

    String name();
}
