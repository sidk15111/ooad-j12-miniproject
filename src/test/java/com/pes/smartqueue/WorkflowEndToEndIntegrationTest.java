package com.pes.smartqueue;

import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.queue.QueueStatus;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;
import com.pes.smartqueue.repository.AppointmentRepository;
import com.pes.smartqueue.repository.QueueEntryRepository;
import com.pes.smartqueue.repository.ServiceSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private ServiceSessionRepository serviceSessionRepository;

    @BeforeEach
    void cleanDb() {
        serviceSessionRepository.deleteAll();
        queueEntryRepository.deleteAll();
        appointmentRepository.deleteAll();
    }

    @Test
    void addWalkInAtReception_reflectsInReceptionAndStaffViews() throws Exception {
        mockMvc.perform(post("/reception/queue/walk-in")
                .with(csrf())
            .with(user("reception").roles("RECEPTIONIST"))
                .param("customerName", "Walk In One"))
            .andExpect(status().is3xxRedirection());

        QueueEntry waitingEntry = queueEntryRepository.findAll().get(0);
        assertEquals(QueueStatus.WAITING, waitingEntry.getStatus());

        mockMvc.perform(post("/staff/availability/available")
                .with(csrf())
                .with(user("staff").roles("SERVICE_STAFF")))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/reception/queue/{id}/assign", waitingEntry.getId())
                .with(csrf())
                .with(user("reception").roles("RECEPTIONIST"))
                .param("staffUsername", "staff"))
            .andExpect(status().is3xxRedirection());

        QueueEntry inProgress = queueEntryRepository.findById(waitingEntry.getId()).orElseThrow();
        assertEquals(QueueStatus.IN_PROGRESS, inProgress.getStatus());

        mockMvc.perform(get("/reception/queue")
            .with(user("reception").roles("RECEPTIONIST")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("entries", hasSize(1)))
            .andExpect(model().attribute("orderedQueue", hasSize(1)));

        mockMvc.perform(get("/staff/sessions")
            .with(user("staff").roles("SERVICE_STAFF")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("queueSnapshot", hasSize(1)));

        long totalEntries = queueEntryRepository.count();
        assertEquals(1L, totalEntries);
    }

    @Test
    void fullJourney_customerToReceptionToStaff_completesQueueEntry() throws Exception {
        String slotTime = LocalDateTime.now().plusHours(1).withNano(0).toString();

        mockMvc.perform(post("/customer/appointments")
                .with(csrf())
            .with(user("customer").roles("CUSTOMER"))
                .param("slotTime", slotTime))
            .andExpect(status().is3xxRedirection());

        Appointment appointment = appointmentRepository.findAll().get(0);

        mockMvc.perform(post("/customer/appointments/{id}/confirm", appointment.getId())
                .with(csrf())
            .with(user("customer").roles("CUSTOMER")))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/reception/appointments/{id}/checkin", appointment.getId())
                .with(csrf())
            .with(user("reception").roles("RECEPTIONIST")))
            .andExpect(status().is3xxRedirection());

        Appointment checkedIn = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CHECKED_IN, checkedIn.getStatus());

        mockMvc.perform(post("/staff/availability/available")
                .with(csrf())
            .with(user("staff").roles("SERVICE_STAFF")))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/reception/queue/from-appointment/{appointmentId}", appointment.getId())
                .with(csrf())
            .with(user("reception").roles("RECEPTIONIST")))
            .andExpect(status().is3xxRedirection());

        QueueEntry waitingEntry = queueEntryRepository.findAll().get(0);
        assertEquals(QueueStatus.WAITING, waitingEntry.getStatus());

        mockMvc.perform(post("/reception/queue/{id}/assign", waitingEntry.getId())
                .with(csrf())
                .with(user("reception").roles("RECEPTIONIST"))
                .param("staffUsername", "staff"))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/staff/assigned/complete")
                .with(csrf())
            .with(user("staff").roles("SERVICE_STAFF")))
            .andExpect(status().is3xxRedirection());

        List<QueueEntry> allEntries = queueEntryRepository.findAll();
        assertEquals(1, allEntries.size());
        assertEquals(QueueStatus.COMPLETED, allEntries.get(0).getStatus());

        ServiceSession updatedSession = serviceSessionRepository.findByStaffUsername("staff").orElseThrow();
        assertEquals(ServiceSessionStatus.ACTIVE, updatedSession.getStatus());
        assertNull(updatedSession.getActiveQueueEntryId());
    }

    @Test
    void staffPageAutoCreatesSessionForAdminDefinedStaff() throws Exception {
        mockMvc.perform(get("/staff/sessions")
                .with(user("staff").roles("SERVICE_STAFF")))
            .andExpect(status().isOk());

        ServiceSession session = serviceSessionRepository.findByStaffUsername("staff").orElseThrow();
        assertEquals("staff", session.getStaffUsername());
        assertEquals(ServiceSessionStatus.IDLE, session.getStatus());
    }

    @Test
    void customerSeesOnlyOwnAppointments() throws Exception {
        mockMvc.perform(post("/customer/appointments")
                .with(csrf())
                .with(user("customer").roles("CUSTOMER"))
                .param("slotTime", LocalDateTime.now().plusHours(2).withNano(0).toString()))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/customer/appointments")
                .with(csrf())
                .with(user("anotherCustomer").roles("CUSTOMER"))
                .param("slotTime", LocalDateTime.now().plusHours(3).withNano(0).toString()))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/customer/appointments")
                .with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("appointments", hasSize(1)))
            .andExpect(model().attribute("appointments", not(hasSize(2))));
    }
}
