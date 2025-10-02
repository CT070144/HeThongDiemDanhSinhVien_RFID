package com.rfid.attendance.repository;

import com.rfid.attendance.entity.DocRfid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocRfidRepository extends JpaRepository<DocRfid, Long> {
    
    Optional<DocRfid> findByRfid(String rfid);
    
    @Query("SELECT d FROM DocRfid d WHERE d.processed = false ORDER BY d.createdAt DESC")
    List<DocRfid> findUnprocessedRfids();
    
    boolean existsByRfid(String rfid);
}
