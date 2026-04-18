package com.pes.smartqueue.service;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;
import com.pes.smartqueue.repository.ServiceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ServiceSessionService {
    private final ServiceSessionRepository serviceSessionRepository;
    private final QueueService queueService;

    public ServiceSessionService(ServiceSessionRepository serviceSessionRepository, QueueService queueService) {
        this.serviceSessionRepository = serviceSessionRepository;
        this.queueService = queueService;
    }

    public ServiceSession create(String staffUsername) {
        ServiceSession session = new ServiceSession(staffUsername);
        return serviceSessionRepository.save(session);
    }

    public void activate(long id) {
        ServiceSession session = require(id);
        ensureSingleActive(session.getStaffUsername(), session.getId());
        session.activate();
        serviceSessionRepository.save(session);
    }

    public void pause(long id) {
        ServiceSession session = require(id);
        session.pause();
        serviceSessionRepository.save(session);
    }

    public void resume(long id) {
        ServiceSession session = require(id);
        ensureSingleActive(session.getStaffUsername(), session.getId());
        session.resume();
        serviceSessionRepository.save(session);
    }

    public void complete(long id) {
        ServiceSession session = require(id);
        if (session.getActiveQueueEntryId() != null) {
            throw new InvalidServiceSessionTransitionException("Complete assigned queue entry before completing session");
        }
        session.complete();
        serviceSessionRepository.save(session);
    }

    public QueueEntry startNextQueueEntry(long id) {
        ServiceSession session = require(id);
        if (session.getStatus() != ServiceSessionStatus.ACTIVE) {
            throw new InvalidServiceSessionTransitionException("Session must be ACTIVE to start queue work");
        }
        if (session.getActiveQueueEntryId() != null) {
            throw new InvalidServiceSessionTransitionException("Session already has an active queue entry");
        }
        QueueEntry started = queueService.startNext();
        session.assignQueueEntry(started.getId());
        serviceSessionRepository.save(session);
        return started;
    }

    public QueueEntry completeAssignedQueueEntry(long id) {
        ServiceSession session = require(id);
        Long queueEntryId = session.getActiveQueueEntryId();
        if (queueEntryId == null) {
            throw new InvalidServiceSessionTransitionException("No assigned queue entry for this session");
        }
        queueService.completeEntry(queueEntryId);
        QueueEntry completed = queueService.getById(queueEntryId);
        session.clearQueueEntry();
        serviceSessionRepository.save(session);
        return completed;
    }

    @Transactional(readOnly = true)
    public QueueEntry getAssignedQueueEntry(long id) {
        ServiceSession session = require(id);
        Long queueEntryId = session.getActiveQueueEntryId();
        if (queueEntryId == null) {
            return null;
        }
        return queueService.getById(queueEntryId);
    }

    @Transactional(readOnly = true)
    public List<ServiceSession> list() {
        return serviceSessionRepository.findAllByOrderByIdAsc();
    }

    private void ensureSingleActive(String staffUsername, Long exceptSessionId) {
        boolean hasAnotherActive = serviceSessionRepository.existsByStaffUsernameAndStatusAndIdNot(
            staffUsername,
            ServiceSessionStatus.ACTIVE,
            exceptSessionId
        );
        if (hasAnotherActive) {
            throw new InvalidServiceSessionTransitionException("Staff member already has an ACTIVE session");
        }
    }

    private ServiceSession require(long id) {
        return serviceSessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service session not found: " + id));
    }
}
