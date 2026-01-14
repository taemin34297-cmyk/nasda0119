package com.example.nasda.controller;

import com.example.nasda.domain.CommentEntity;
import com.example.nasda.domain.UserEntity;
import com.example.nasda.domain.UserRepository;
import com.example.nasda.dto.comment.CommentCreateRequestDto;
import com.example.nasda.repository.CommentRepository;
import com.example.nasda.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository; // 👈 1. 이 줄을 꼭 추가하세요!

    // =========================
    // 로그인 사용자 정보
    // =========================
    private String getLoginIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) return null;

        String loginId = auth.getName();
        return (loginId == null || loginId.isBlank()) ? null : loginId;
    }

    private Integer getCurrentUserIdOrNull() {
        String loginId = getLoginIdOrNull();
        if (loginId == null) return null;

        return userRepository.findByLoginId(loginId)
                .map(UserEntity::getUserId)
                .orElse(null);
    }

    // =========================
    // ✅ 추가: 내 댓글 목록 조회 (페이징)
    // =========================
    @GetMapping("/comments/my")
    public String myComments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        // 한 페이지에 10개씩, 최신순(createdAt DESC) 정렬
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());

        // 서비스에서 Page<CommentEntity> 형태로 가져옴
        Page<CommentEntity> commentPage = commentService.findByUserId(currentUserId, pageable);

        model.addAttribute("comments", commentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", commentPage.getTotalPages());

        return "comment/my-list";
    }

    // =========================
    // 댓글 작성/삭제/수정
    // =========================

    @PostMapping("/comments")
    public String create(
            @Valid @ModelAttribute CommentCreateRequestDto req,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        commentService.createComment(req.postId(), currentUserId, req.content());

        return "redirect:/posts/" + req.postId()
                + "?page=0"
                + "&size=" + size
                + "#comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String delete(
            @PathVariable("id") Integer commentId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        Integer postId = commentService.deleteComment(commentId, currentUserId);

        return "redirect:/posts/" + postId
                + "?page=0"
                + "&size=5"
                + "#comments";
    }

    @PostMapping("/comments/{id}/edit")
    public String edit(
            @PathVariable("id") Integer commentId,
            @RequestParam("content") String content,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        Integer postId = commentService.editComment(commentId, currentUserId, content);

        return "redirect:/posts/" + postId
                + "?page=" + page
                + "&size=" + size
                + "#comments";
    }
    @GetMapping("/comments/{id}/go")
    public String goToComment(@PathVariable("id") Integer commentId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다."));

        Integer postId = comment.getPost().getPostId();
        int pageSize = 5;

        int page = commentService.getPageNumberByCommentId(postId, commentId, pageSize);

        // 💡 인텔리제이 콘솔창에 숫자가 몇이 찍히는지 확인해보세요!
        System.out.println("디버깅 - 댓글ID: " + commentId + ", 계산된 페이지: " + page);

        return "redirect:/posts/" + postId + "?page=" + page + "#comment-" + commentId;
    }

}