package com.server.talkup_be.repo;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.entity.Analysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalysisRepo extends JpaRepository<Analysis, UUID> {
    // 전체 개수 조회 (totalPages 계산용)
    @Query(value = "SELECT COUNT(*) FROM analysis a WHERE " +
            "(:folderId IS NULL OR a.folder_Id = :folderId) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:keyWord IS NULL OR LOWER(a.title) LIKE LOWER(:keyWord)) AND " +
            "a.user_Id = :userId", nativeQuery = true)
    Integer countFilteredAnalyses(String userId, String folderId, Integer type, String keyWord);

    // 연습기록 미리보기 조회
    @Query("SELECT new com.server.talkup_be.dto.AnalysisDto$AnalysisCardnews(" +
            "a.id, a.title, a.description, a.type, a.createdAt) " +
            "FROM Analysis a WHERE " +
            "a.userId = :userId AND " +
            "(:folderId IS NULL OR a.folderId = :folderId) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:keyWord IS NULL OR a.title LIKE :keyWord)")
    List<AnalysisDto.AnalysisCardnews> findAnalyses(String userId, String folderId, Integer type, String keyWord, Pageable pageable);
}
