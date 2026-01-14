package com.example.nasda.repository;

import com.example.nasda.domain.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Integer> {

    long countByUser_UserId(Integer userId);

    // ✅ 추가: 내 전체 포스트 목록 조회를 위해 꼭 필요합니다.
    List<PostEntity> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    List<PostEntity> findTop4ByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    List<PostEntity> findAllByOrderByCreatedAtDesc();

    List<PostEntity> findTop30ByOrderByCreatedAtDesc();

    @Query("""
        select p
        from PostEntity p
        join fetch p.user
        join fetch p.category
        order by p.createdAt desc
    """)
    List<PostEntity> findAllWithUserAndCategoryOrderByCreatedAtDesc();
}