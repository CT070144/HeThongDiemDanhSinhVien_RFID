package com.rfid.attendance.repository;

import com.rfid.attendance.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, String> {
}

