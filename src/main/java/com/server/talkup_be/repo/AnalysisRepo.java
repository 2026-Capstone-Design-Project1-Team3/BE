package com.server.talkup_be.repo;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.entity.Analysis;
import com.server.talkup_be.entity.Folder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    Integer countFilteredAnalyses(UUID userId, UUID folderId, Integer type, String keyWord);

    // 연습기록 미리보기 조회
    @Query("SELECT new com.server.talkup_be.dto.AnalysisDto$AnalysisCardnewsInfo$AnalysisCardnews(" +
            "a.id, a.title, a.type, a.createdAt) " +
            "FROM Analysis a WHERE " +
            "a.userId = :userId AND " +
            "(:folderId IS NULL OR a.folderId = :folderId) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:keyWord IS NULL OR a.title LIKE :keyWord)")
    List<AnalysisDto.AnalysisCardnewsInfo.AnalysisCardnews> findAnalyses(UUID userId, UUID folderId, Integer type, String keyWord, Pageable pageable);

    // userId가 가진 연습기록 전체 개수 반환
    Integer countByUserId(UUID string);

    // 연습기록 최신 n개 수치들
    @Query("SELECT new com.server.talkup_be.dto.AnalysisDto$AnalysisStatistics$StatisticData(" +
            "a.gazeScore, a.speedScore) " +
            "FROM Analysis a WHERE " +
            "a.userId = :userId " +
            "ORDER BY a.createdAt DESC")
    List<AnalysisDto.AnalysisStatistics.StatisticData> findStatistics(
            @Param("userId") UUID userId,
            Pageable pageable
    );
    // 연습기록 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM Analysis a WHERE a.folderId IN :folderIds")
    void deleteByFolderIdIn(List<UUID> folderIds);

}
