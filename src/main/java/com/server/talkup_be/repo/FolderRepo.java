package com.server.talkup_be.repo;

import com.server.talkup_be.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FolderRepo extends JpaRepository<Folder, UUID> {
    // 전체 개수 조회 (totalPages 계산용)
    @Query(value = "SELECT COUNT(*) FROM folder f WHERE " +
            "(:type IS NULL OR f.type = :type) AND " +
            "(:keyWord IS NULL OR LOWER(f.title) LIKE LOWER(:keyWord)) AND " +
            "f.user_Id = :userId", nativeQuery = true)
    Integer countFilteredFolders(String userId, Integer type, String keyWord);
}
