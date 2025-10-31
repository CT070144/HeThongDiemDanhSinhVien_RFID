package com.rfid.attendance.repository;


import com.rfid.attendance.entity.WebSocketSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface WebSocketSessionRepository extends JpaRepository<WebSocketSession,String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM WebSocketSession s WHERE TRIM(s.socketSessionId) = TRIM(:socketSessionId)")
    void deleteBySocketSessionId(@Param("socketSessionId") String socketSessionId);
}
