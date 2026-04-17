package com.pes.smartqueue.service;

import com.pes.smartqueue.exception.InvalidServiceSessionTransitionException;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ServiceSessionService {
    private final AtomicLong idSequence = new AtomicLong(1L);
    private final Map<Long, ServiceSession> sessions = new ConcurrentHashMap<>();

    public synchronized ServiceSession create(String staffUsername) {
        long id = idSequence.getAndIncrement();
        ServiceSession session = new ServiceSession(id, staffUsername);
        sessions.put(id, session);
        return session;
    }

    public synchronized void activate(long id) {
        ServiceSession session = require(id);
        ensureSingleActive(session.getStaffUsername(), id);
        session.activate();
    }

    public synchronized void pause(long id) {
        require(id).pause();
    }

    public synchronized void resume(long id) {
        ServiceSession session = require(id);
        ensureSingleActive(session.getStaffUsername(), id);
        session.resume();
    }

    public synchronized void complete(long id) {
        require(id).complete();
    }

    public synchronized List<ServiceSession> list() {
        return sessions.values().stream()
            .sorted(Comparator.comparing(ServiceSession::getId))
            .toList();
    }

    private void ensureSingleActive(String staffUsername, long exceptSessionId) {
        boolean hasAnotherActive = sessions.values().stream()
            .filter(session -> session.getId() != exceptSessionId)
            .filter(session -> session.getStaffUsername().equals(staffUsername))
            .anyMatch(session -> session.getStatus() == ServiceSessionStatus.ACTIVE);
        if (hasAnotherActive) {
            throw new InvalidServiceSessionTransitionException("Staff member already has an ACTIVE session");
        }
    }

    private ServiceSession require(long id) {
        ServiceSession session = sessions.get(id);
        if (session == null) {
            throw new IllegalArgumentException("Service session not found: " + id);
        }
        return session;
    }
}
