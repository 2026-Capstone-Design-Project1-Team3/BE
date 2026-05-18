package com.server.talkup_be.repo;

import com.server.talkup_be.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalysisRepo extends JpaRepository<Analysis, UUID> {
    // 전체 개수 조회 (totalPages 계산용)
    @Query(value = "SELECT COUNT(*) FROM analysis a WHERE " +
            "(:folderId IS NULL OR a.folder_Id = :folderId) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:keyWord IS NULL OR LOWER(a.title) LIKE LOWER(:keyWord)) AND " +
            "a.user_Id = :userId", nativeQuery = true)
    Integer countFilteredFolders(String userId, String folderId, Integer type, String keyWord);
}
