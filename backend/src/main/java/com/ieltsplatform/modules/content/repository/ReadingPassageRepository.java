package com.ieltsplatform.modules.content.repository;

import com.ieltsplatform.modules.content.entities.ReadingPassage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReadingPassageRepository extends JpaRepository<ReadingPassage, UUID> {

    // 1. Tìm tất cả bài đọc theo cấp độ (Ví dụ: tìm tất cả bài "B2")
    List<ReadingPassage> findByLevel(String level);

    // 2. Tìm tất cả bài đọc theo chủ đề (Ví dụ: tìm tất cả bài chủ đề "Environment")
    List<ReadingPassage> findByTopic(String topic);

    // 3. Tìm bài đọc kết hợp cả Cấp độ & Chủ đề, có hỗ trợ Phân trang (Pagination)
    Page<ReadingPassage> findByLevelAndTopic(String level, String topic, Pageable pageable);
}
