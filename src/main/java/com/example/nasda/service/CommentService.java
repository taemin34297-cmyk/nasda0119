package com.example.nasda.service;

import com.example.nasda.domain.CommentEntity;
import com.example.nasda.domain.PostEntity;
import com.example.nasda.dto.comment.CommentViewDto;
import com.example.nasda.repository.CommentRepository;
import com.example.nasda.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public Page<CommentViewDto> getCommentsPage(Integer postId, int page, int size, Integer currentUserId) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);

        return commentRepository
                .findByPost_PostIdOrderByCreatedAtDesc(postId, PageRequest.of(safePage, safeSize))
                .map(e -> new CommentViewDto(
                        e.getCommentId(),
                        e.getContent(),
                        // ✅ 유저 테이블 조인 전: user_id를 닉네임처럼 표시
                        "사용자" + e.getUserId(),
                        e.getCreatedAt(),
                        e.getUserId().equals(currentUserId)
                ));
    }

    @Transactional
    public Integer createComment(Integer postId, Integer userId, String content) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글: " + postId));

        // content 정리(선택)
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("댓글 내용이 비어있습니다.");
        }
        if (trimmed.length() > 500) {
            throw new IllegalArgumentException("댓글은 최대 500자까지 가능합니다.");
        }

        CommentEntity c = new CommentEntity();

        // ✅ 현재 CommentEntity가 setter가 없으니 "생성용 생성자/팩토리"가 필요함
        // 지금 엔티티는 NoArgsConstructor + private 필드라 여기서 값 세팅이 불가.
        // 그래서 CommentEntity에 create() 팩토리를 추가해주자 (아래 3번 참고)

        c = CommentEntity.create(post, userId, trimmed);
        CommentEntity saved = commentRepository.save(c);
        return saved.getCommentId();
    }

    public int getLastPageIndex(Integer postId, int size) {
        int safeSize = Math.max(1, size);
        long total = commentRepository.countByPost_PostId(postId);

        // total=0이면 lastPage=0
        if (total <= 0) return 0;

        // 예: total=15, size=5 -> (15-1)/5 = 2 (0-based)
        return (int) ((total - 1) / safeSize);
    }

    @Transactional
    public Integer deleteComment(Integer commentId, Integer currentUserId) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다. id=" + commentId));

        // ✅ 권한 체크(본인만 삭제 가능)
        if (!comment.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("본인 댓글만 삭제할 수 있습니다.");
        }

        Integer postId = comment.getPost().getPostId();
        commentRepository.delete(comment); // ✅ 물리 삭제
        return postId;
    }

    @Transactional
    public Integer editComment(Integer commentId, Integer currentUserId, String newContent) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다. id=" + commentId));

        if (!comment.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("본인 댓글만 수정할 수 있습니다.");
        }

        String trimmed = newContent == null ? "" : newContent.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("댓글 내용이 비어있습니다.");
        if (trimmed.length() > 500) throw new IllegalArgumentException("댓글은 최대 500자까지 가능합니다.");

        comment.edit(trimmed); // 아래 2)에서 엔티티 메서드 추가
        return comment.getPost().getPostId();
    }

    // CommentService.java 내부에 추가
    @Transactional(readOnly = true)
    public Page<CommentEntity> findByUserId(Integer userId, Pageable pageable) {
        // ⚠️ Repository에서도 findByUserId(Integer userId, Pageable pageable)로 이름이 동일해야 합니다.
        return commentRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public int getPageNumberByCommentId(Integer postId, Integer commentId, int pageSize) {
        // 1. DESC(최신순)로 정렬하여 리스트를 가져옵니다.
        List<CommentEntity> allComments = commentRepository.findByPost_PostIdOrderByCreatedAtDesc(postId);

        int index = 0;
        for (int i = 0; i < allComments.size(); i++) {
            if (allComments.get(i).getCommentId().equals(commentId)) {
                index = i;
                break;
            }
        }

        System.out.println("디버깅 - 전체 댓글 수: " + allComments.size());
        System.out.println("디버깅 - 내 댓글의 순서(Index): " + index);

        return index / pageSize;
    }
}