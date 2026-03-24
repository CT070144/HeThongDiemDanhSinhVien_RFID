package com.rfid.attendance.repository;

import com.rfid.attendance.entity.CaLam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaLamRepository extends JpaRepository<CaLam, Long> {
    Optional<CaLam> findByMaCa(Integer maCa);

    List<CaLam> findAllByOrderByMaCaAsc();
}

