package com.pes.smartqueue.repository;

import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceSessionRepository extends JpaRepository<ServiceSession, Long> {
    List<ServiceSession> findAllByOrderByIdAsc();

    Optional<ServiceSession> findByStaffUsername(String staffUsername);

    long countByStatus(ServiceSessionStatus status);

    boolean existsByStaffUsernameAndStatusAndIdNot(String staffUsername, ServiceSessionStatus status, Long id);
}
