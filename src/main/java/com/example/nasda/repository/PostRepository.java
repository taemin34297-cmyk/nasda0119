package com.example.nasda.repository;

import com.example.nasda.domain.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Integer> {

    long countByUser_UserId(Integer userId);

    // ✅ 내 전체 포스트 목록 조회
    List<PostEntity> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    List<PostEntity> findTop4ByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    List<PostEntity> findAllByOrderByCreatedAtDesc();

    List<PostEntity> findTop30ByOrderByCreatedAtDesc();

    // ✅ 카테고리 필터 + 페이징
    Page<PostEntity> findByCategory_CategoryNameOrderByCreatedAtDesc(String categoryName, Pageable pageable);

    // ✅ 전체 + 페이징
    Page<PostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        select p
        from PostEntity p
        join fetch p.user
        join fetch p.category
        order by p.createdAt desc
    """)
    List<PostEntity> findAllWithUserAndCategoryOrderByCreatedAtDesc();

    // =========================
    // ✅ [추가] 검색 기능용
    // =========================
    List<PostEntity> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    List<PostEntity> findByDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    List<PostEntity> findByUser_NicknameContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    List<PostEntity> findByCategory_CategoryNameContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    Page<PostEntity> findByUser_UserId(Integer userId, Pageable pageable);}
