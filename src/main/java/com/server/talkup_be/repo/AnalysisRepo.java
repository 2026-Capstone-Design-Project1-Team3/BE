package com.server.talkup_be.repo;

import com.server.talkup_be.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalysisRepo extends JpaRepository<Analysis, UUID> {

}
