package com.example.nasda.service;

import com.example.nasda.domain.CategoryEntity;
import com.example.nasda.domain.PostEntity;
import com.example.nasda.domain.UserEntity;
import com.example.nasda.domain.UserRepository;
import com.example.nasda.dto.post.HomePostDto;
import com.example.nasda.dto.post.PostViewDto;
import com.example.nasda.repository.CategoryRepository;
import com.example.nasda.repository.CommentRepository;
import com.example.nasda.repository.PostImageRepository;
import com.example.nasda.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;

    // 🔹 게시글 단건 조회
    @Transactional(readOnly = true)
    public PostEntity get(Integer postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
    }

    // 🔹 홈 게시글 목록 (최신 30개 + 대표 이미지 1장)
    @Transactional(readOnly = true)
    public List<HomePostDto> getHomePosts() {
        return postRepository.findTop30ByOrderByCreatedAtDesc().stream()
                .map(post -> {
                    String imageUrl = postImageRepository
                            .findFirstByPost_PostIdOrderBySortOrderAsc(post.getPostId())
                            .map(img -> img.getImageUrl())
                            .orElse(null);

                    return new HomePostDto(post.getPostId(), post.getTitle(), imageUrl);
                })
                .toList();
    }

    // 🔹 게시글 생성
    public PostEntity create(Integer userId, Integer categoryId, String title, String description) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리 없음"));

        PostEntity post = PostEntity.builder()
                .user(user)
                .category(category)
                .title(title)
                .description(description)
                .build();

        return postRepository.save(post);
    }

    // 🔹 게시글 수정
    public void update(Integer postId, Integer userId,
                       Integer categoryId, String title, String description) {

        PostEntity post = get(postId);

        if (!post.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("수정 권한 없음");
        }

        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리 없음"));

        post.update(category, title, description);
    }

    // 🔥 게시글 삭제 (FK 해결 핵심)
    public void delete(Integer postId, Integer userId) {
        PostEntity post = get(postId);

        if (!post.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("삭제 권한 없음");
        }

        // ✅ 1. 이미지 삭제
        postImageRepository.deleteByPost_PostId(postId);

        // ✅ 2. 댓글 삭제
        commentRepository.deleteByPost_PostId(postId);

        // ✅ 3. 게시글 삭제
        postRepository.delete(post);
    }
    // PostService.java
    @Transactional(readOnly = true)
    public List<PostEntity> findByUserId(Integer userId) {
        return postRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
    }
    // ✅ 마이페이지: 내 게시글 개수
    @Transactional(readOnly = true)
    public long countMyPosts(Integer userId) {
        return postRepository.countByUser_UserId(userId);
    }

    // ✅ 마이페이지: 내 최근 게시글 목록
    @Transactional(readOnly = true)
    public List<PostViewDto> getMyRecentPosts(Integer userId, int limit) {

        // 지금은 Repository가 Top4 기반이라 limit은 참고값(추후 PageRequest로 개선 가능)
        List<PostEntity> posts = postRepository.findTop4ByUser_UserIdOrderByCreatedAtDesc(userId);

        return posts.stream()
                .map(post -> {
                    List<String> images = postImageRepository
                            .findAllByPost_PostIdOrderBySortOrderAsc(post.getPostId())
                            .stream()
                            .map(img -> img.getImageUrl())
                            .toList();

                    return new PostViewDto(
                            post.getPostId(),
                            post.getTitle(),
                            post.getDescription(), // PostViewDto.content 에 description 매핑
                            post.getCategory().getCategoryName(), // ✅ 여기만 수정!
                            new PostViewDto.AuthorDto(post.getUser().getNickname()),
                            images,
                            post.getCreatedAt(),
                            true
                    );
                })
                .toList();
    }
}
