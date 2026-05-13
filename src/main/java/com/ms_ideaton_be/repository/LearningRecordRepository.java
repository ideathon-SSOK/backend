package com.ms_ideaton_be.repository;

import com.ms_ideaton_be.domain.LearningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LearningRecordRepository extends JpaRepository<LearningRecord, Long> {
}