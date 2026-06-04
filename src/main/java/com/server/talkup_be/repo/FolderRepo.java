package com.server.talkup_be.repo;

import com.server.talkup_be.dto.FolderDto;
import com.server.talkup_be.entity.Folder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolderRepo extends JpaRepository<Folder, UUID> {
    // 전체 개수 조회 (totalPages 계산용)
    @Query(value = "SELECT COUNT(*) FROM folder f WHERE " +
            "(:type IS NULL OR f.type = :type) AND " +
            "(:keyWord IS NULL OR LOWER(f.title) LIKE LOWER(:keyWord)) AND " +
            "f.user_Id = :userId", nativeQuery = true)
    Integer countFilteredFolders(UUID userId, Integer type, String keyWord);

    // 폴더 미리보기 조회
    @Query("SELECT new com.server.talkup_be.dto.FolderDto$FolderCardnewsInfo(" +
            "f.id, f.title, f.type, f.updatedAt, COUNT(a.id)) " +
            "FROM Folder f " +
            "LEFT JOIN Analysis a ON f.id = a.folderId " +
            "WHERE f.userId = :userId " +
            "AND (:type IS NULL OR f.type = :type) " +
            "AND (:keyWord IS NULL OR f.title LIKE :keyWord) " +
            "GROUP BY f.id, f.title, f.type, f.updatedAt")
    List<FolderDto.FolderCardnewsInfo> findFolders(@Param("userId") UUID userId,
                                           @Param("type") Integer type,
                                           @Param("keyWord") String keyWord,
                                           Pageable pageable);
    // userId기반 folder 찾기
    List<Folder> findAllByUserId(UUID string);
}