package com.pes.smartqueue.service;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;
import com.pes.smartqueue.repository.ServiceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ServiceSessionService {
    private final ServiceSessionRepository serviceSessionRepository;
    private final QueueService queueService;
    private final UserManagementService userManagementService;

    public ServiceSessionService(ServiceSessionRepository serviceSessionRepository,
                                 QueueService queueService,
                                 UserManagementService userManagementService) {
        this.serviceSessionRepository = serviceSessionRepository;
        this.queueService = queueService;
        this.userManagementService = userManagementService;
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

    public QueueEntry completeAssignedQueueEntryForStaff(String staffUsername) {
        ServiceSession session = requireStaffSession(staffUsername);
        Long queueEntryId = session.getActiveQueueEntryId();
        if (queueEntryId == null) {
            throw new InvalidServiceSessionTransitionException("No assigned patient for this staff member");
        }
        queueService.completeEntry(queueEntryId);
        QueueEntry completed = queueService.getById(queueEntryId);
        session.clearQueueEntry();
        serviceSessionRepository.save(session);
        return completed;
    }

    public void releaseAssignedQueueEntry(long id) {
        ServiceSession session = require(id);
        Long queueEntryId = session.getActiveQueueEntryId();
        if (queueEntryId == null) {
            throw new InvalidServiceSessionTransitionException("No assigned queue entry for this session");
        }
        queueService.requeueEntry(queueEntryId);
        session.clearQueueEntry();
        serviceSessionRepository.save(session);
    }

    public QueueEntry getAssignedQueueEntry(long id) {
        ServiceSession session = require(id);
        Long queueEntryId = session.getActiveQueueEntryId();
        if (queueEntryId == null) {
            return null;
        }
        QueueEntry assigned = queueService.findByIdOrNull(queueEntryId);
        if (assigned == null) {
            session.clearQueueEntry();
            serviceSessionRepository.save(session);
            return null;
        }
        return assigned;
    }

    public QueueEntry getAssignedQueueEntryForStaff(String staffUsername) {
        ServiceSession session = requireStaffSession(staffUsername);
        Long queueEntryId = session.getActiveQueueEntryId();
        if (queueEntryId == null) {
            return null;
        }
        return queueService.findByIdOrNull(queueEntryId);
    }

    public void setStaffAvailability(String staffUsername, boolean available) {
        ServiceSession session = requireStaffSession(staffUsername);
        if (available) {
            if (session.getStatus() == ServiceSessionStatus.ACTIVE) {
                return;
            }
            if (session.getStatus() == ServiceSessionStatus.PAUSED) {
                session.resume();
            } else if (session.getStatus() == ServiceSessionStatus.IDLE) {
                ensureSingleActive(session.getStaffUsername(), session.getId());
                session.activate();
            } else {
                throw new InvalidServiceSessionTransitionException("Cannot set availability from " + session.getStatus());
            }
        } else {
            if (session.getActiveQueueEntryId() != null) {
                throw new InvalidServiceSessionTransitionException("Cannot mark unavailable while a patient is assigned");
            }
            if (session.getStatus() == ServiceSessionStatus.PAUSED || session.getStatus() == ServiceSessionStatus.IDLE) {
                return;
            }
            if (session.getStatus() == ServiceSessionStatus.ACTIVE) {
                session.pause();
            } else {
                throw new InvalidServiceSessionTransitionException("Cannot set availability from " + session.getStatus());
            }
        }
        serviceSessionRepository.save(session);
    }

    public QueueEntry assignQueueEntryToStaff(String staffUsername, long queueEntryId) {
        ServiceSession session = requireStaffSession(staffUsername);
        if (session.getStatus() != ServiceSessionStatus.ACTIVE || session.getActiveQueueEntryId() != null) {
            throw new InvalidServiceSessionTransitionException("Selected staff is not available for assignment");
        }
        QueueEntry entry = queueService.getById(queueEntryId);
        if (entry.getStatus() != com.pes.smartqueue.model.queue.QueueStatus.IN_PROGRESS) {
            throw new InvalidServiceSessionTransitionException("Queue entry must be IN_PROGRESS before assignment");
        }
        session.assignQueueEntry(queueEntryId);
        serviceSessionRepository.save(session);
        return entry;
    }

    @Transactional(readOnly = true)
    public List<String> listAvailableStaffUsernames() {
        return userManagementService.listUsers().stream()
            .filter(user -> "SERVICE_STAFF".equals(user.getRole()) && user.isActive())
            .map(user -> user.getUsername())
            .filter(username -> {
                ServiceSession session = serviceSessionRepository.findByStaffUsername(username).orElse(null);
                return session != null
                    && session.getStatus() == ServiceSessionStatus.ACTIVE
                    && session.getActiveQueueEntryId() == null;
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, String> assignedStaffByQueueEntryId() {
        Map<Long, String> assigned = new LinkedHashMap<>();
        for (ServiceSession session : serviceSessionRepository.findAllByOrderByIdAsc()) {
            if (session.getActiveQueueEntryId() != null) {
                assigned.put(session.getActiveQueueEntryId(), session.getStaffUsername());
            }
        }
        return assigned;
    }

    public ServiceSession requireStaffSession(String staffUsername) {
        if (!userManagementService.listUsers().stream().anyMatch(user -> user.getUsername().equals(staffUsername) && "SERVICE_STAFF".equals(user.getRole()))) {
            throw new InvalidServiceSessionTransitionException("Unknown service staff account: " + staffUsername);
        }
        Optional<ServiceSession> existing = serviceSessionRepository.findByStaffUsername(staffUsername);
        if (existing.isPresent()) {
            return existing.get();
        }
        ServiceSession created = new ServiceSession(staffUsername);
        return serviceSessionRepository.save(created);
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
