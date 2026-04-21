package com.pes.smartqueue;

import com.pes.smartqueue.exception.InvalidAppointmentTransitionException;
import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;
import com.pes.smartqueue.model.queue.EntryType;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.queue.QueueStatus;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.repository.AppointmentRepository;
import com.pes.smartqueue.repository.QueueEntryRepository;
import com.pes.smartqueue.repository.ServiceSessionRepository;
import com.pes.smartqueue.service.AppointmentService;
import com.pes.smartqueue.service.QueueService;
import com.pes.smartqueue.service.ServiceSessionService;
import com.pes.smartqueue.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceRulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueService queueService;

    @Autowired
    private ServiceSessionService serviceSessionService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private ServiceSessionRepository serviceSessionRepository;

    @Autowired
    private UserManagementService userManagementService;

    @BeforeEach
    void cleanDb() {
        serviceSessionRepository.deleteAll();
        queueEntryRepository.deleteAll();
        appointmentRepository.deleteAll();
        userManagementService.resetSeedUsersToActive();
    }

    @Test
    void deactivatedCustomer_isBlockedFromCustomerPage() throws Exception {
        userManagementService.deactivate("customer");

        mockMvc.perform(get("/customer/appointments")
                .with(SecurityMockMvcRequestPostProcessors.user("customer").roles("CUSTOMER")))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void addAppointmentEntry_twice_throwsError() {
        queueService.addAppointmentEntry(1001L, "Ravi");

        assertThrows(IllegalArgumentException.class,
            () -> queueService.addAppointmentEntry(1001L, "Ravi"));
    }

    @Test
    void sameStaffCannotHaveTwoActiveSessions() {
        ServiceSession first = serviceSessionService.create("staffA");
        ServiceSession second = serviceSessionService.create("staffA");

        serviceSessionService.activate(first.getId());

        assertThrows(InvalidServiceSessionTransitionException.class,
            () -> serviceSessionService.activate(second.getId()));
    }

    @Test
    void sessionCannotStartNextWhenNotActive() {
        ServiceSession session = serviceSessionService.create("staffB");
        queueService.addEntry("WalkIn", EntryType.WALK_IN);

        assertThrows(InvalidServiceSessionTransitionException.class,
            () -> serviceSessionService.startNextQueueEntry(session.getId()));
    }

    @Test
    void checkInBeforeConfirm_isRejectedByState() {
        Appointment appointment = appointmentService.create("Maya", LocalDateTime.now().plusHours(2));

        assertThrows(InvalidAppointmentTransitionException.class,
            () -> appointmentService.checkIn(appointment.getId()));
    }

    @Test
    void listingAppointments_expiresPastDueCreatedOrConfirmed() throws Exception {
        appointmentService.create("LateCustomer", LocalDateTime.now().minusHours(2));

        mockMvc.perform(get("/customer/appointments")
                .with(SecurityMockMvcRequestPostProcessors.user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk());

        Appointment expired = appointmentRepository.findAll().get(0);
        assertEquals(AppointmentStatus.EXPIRED, expired.getStatus());
    }

    @Test
    void enqueueCancelledOrExpiredAppointment_isBlockedAtReceptionFlow() throws Exception {
        Appointment cancelled = appointmentService.create("CancelledCase", LocalDateTime.now().plusHours(2));
        appointmentService.cancel(cancelled.getId());

        mockMvc.perform(post("/reception/queue/from-appointment/{appointmentId}", cancelled.getId())
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("reception").roles("RECEPTIONIST")))
            .andExpect(status().is3xxRedirection());

        Appointment expired = appointmentService.create("ExpiredCase", LocalDateTime.now().minusHours(3));
        appointmentService.expirePastDue();

        mockMvc.perform(post("/reception/queue/from-appointment/{appointmentId}", expired.getId())
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("reception").roles("RECEPTIONIST")))
            .andExpect(status().is3xxRedirection());

        assertEquals(0L, queueEntryRepository.count());
    }

    @Test
    void doubleClickAddToQueue_keepsSingleEntryForOneAppointment() throws Exception {
        Appointment appointment = appointmentService.create("DoubleClick", LocalDateTime.now().plusHours(2));
        appointmentService.confirm(appointment.getId());
        appointmentService.checkIn(appointment.getId());

        mockMvc.perform(post("/reception/queue/from-appointment/{appointmentId}", appointment.getId())
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("reception").roles("RECEPTIONIST")))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/reception/queue/from-appointment/{appointmentId}", appointment.getId())
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("reception").roles("RECEPTIONIST")))
            .andExpect(status().is3xxRedirection());

        assertEquals(1L, queueEntryRepository.count());
    }

    @Test
    void orphanedInProgressEntry_canBeReleasedBackToWaiting() {
        queueService.addEntry("OrphanCase", EntryType.WALK_IN);
        ServiceSession session = serviceSessionService.create("staff-orphan");
        serviceSessionService.activate(session.getId());

        QueueEntry assigned = serviceSessionService.startNextQueueEntry(session.getId());
        assertEquals(QueueStatus.IN_PROGRESS, queueService.getById(assigned.getId()).getStatus());

        serviceSessionService.releaseAssignedQueueEntry(session.getId());

        QueueEntry requeued = queueService.getById(assigned.getId());
        ServiceSession updated = serviceSessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(QueueStatus.WAITING, requeued.getStatus());
        assertEquals(null, updated.getActiveQueueEntryId());
    }

    @Test
    void doublePullRace_assignsDistinctQueueEntries() throws Exception {
        queueService.addEntry("RaceOne", EntryType.WALK_IN);
        queueService.addEntry("RaceTwo", EntryType.WALK_IN);

        ServiceSession s1 = serviceSessionService.create("staff-race-1");
        ServiceSession s2 = serviceSessionService.create("staff-race-2");
        serviceSessionService.activate(s1.getId());
        serviceSessionService.activate(s2.getId());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<QueueEntry>> futures = new ArrayList<>();

        futures.add(executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return serviceSessionService.startNextQueueEntry(s1.getId());
        }));
        futures.add(executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return serviceSessionService.startNextQueueEntry(s2.getId());
        }));

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        Set<Long> queueIds = new HashSet<>();
        for (Future<QueueEntry> future : futures) {
            QueueEntry entry = future.get(10, TimeUnit.SECONDS);
            assertNotNull(entry);
            queueIds.add(entry.getId());
        }

        executor.shutdownNow();

        assertEquals(2, queueIds.size());
    }
}
