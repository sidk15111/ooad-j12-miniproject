package com.pes.smartqueue.repository;

import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.model.session.ServiceSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceSessionRepository extends JpaRepository<ServiceSession, Long> {
    List<ServiceSession> findAllByOrderByIdAsc();

    boolean existsByStaffUsernameAndStatusAndIdNot(String staffUsername, ServiceSessionStatus status, Long id);
}
