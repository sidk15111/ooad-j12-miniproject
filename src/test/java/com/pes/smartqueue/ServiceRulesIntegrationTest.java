package com.pes.smartqueue;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.queue.EntryType;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.repository.AppointmentRepository;
import com.pes.smartqueue.repository.QueueEntryRepository;
import com.pes.smartqueue.repository.ServiceSessionRepository;
import com.pes.smartqueue.service.QueueService;
import com.pes.smartqueue.service.ServiceSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ServiceRulesIntegrationTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private ServiceSessionService serviceSessionService;

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
}
