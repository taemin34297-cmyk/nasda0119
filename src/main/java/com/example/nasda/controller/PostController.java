package com.example.nasda.controller;

import com.example.nasda.domain.CategoryEntity;
import com.example.nasda.domain.PostEntity;
import com.example.nasda.domain.UserEntity;
import com.example.nasda.domain.UserRepository;
import com.example.nasda.dto.post.PostCreateRequestDto;
import com.example.nasda.dto.post.PostViewDto;
import com.example.nasda.repository.CategoryRepository;
import com.example.nasda.service.CommentService;
import com.example.nasda.service.PostImageService;
import com.example.nasda.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CategoryRepository categoryRepository;
    private final CommentService commentService;
    private final PostImageService postImageService;
    private final UserRepository userRepository;

    // 1. 글 작성 페이지 (GET)
    @GetMapping("/posts/create")
    public String createForm(Model model) {
        model.addAttribute("postCreateRequestDto", new PostCreateRequestDto("", "", ""));
        model.addAttribute("categories", categoryRepository.findAll());

        String nickname = getCurrentNicknameOrNull();
        model.addAttribute("username", nickname == null ? "게스트" : nickname);

        return "post/create"; // templates/post/create.html
    }

    // 2. 게시글 상세 보기 (ID 변수 처리)
    // 기존의 viewCompat과 viewPost를 하나로 합쳐서 깔끔하게 정리했습니다.
    @GetMapping("/posts/{postId}")
    public String viewPost(
            @PathVariable("postId") String postIdStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model
    ) {
        try {
            // "create" 문자가 들어오면 상세보기가 아닌 글쓰기 폼으로 토스
            if ("create".equals(postIdStr)) {
                return "forward:/posts/create";
            }

            Integer postId = Integer.parseInt(postIdStr);

            // 데이터 조회 로직
            PostEntity entity = postService.get(postId);
            List<String> imageUrls = postImageService.getImageUrls(postId);
            Integer currentUserId = getCurrentUserIdOrNull();

            boolean isOwner = currentUserId != null
                    && entity.getUser() != null
                    && currentUserId.equals(entity.getUser().getUserId());

            PostViewDto post = new PostViewDto(
                    entity.getPostId(),
                    entity.getTitle(),
                    entity.getDescription(),
                    entity.getCategory().getCategoryName(),
                    new PostViewDto.AuthorDto(entity.getUser().getNickname()),
                    imageUrls,
                    entity.getCreatedAt(),
                    isOwner
            );

            var commentsPage = commentService.getCommentsPage(postId, page, size, currentUserId);
            model.addAttribute("post", post);
            model.addAttribute("comments", commentsPage.getContent());
            model.addAttribute("commentsPage", commentsPage);

            String nickname = getCurrentNicknameOrNull();
            model.addAttribute("username", nickname == null ? "게스트" : nickname);

            return "post/view"; // templates/post/view.html

        } catch (NumberFormatException e) {
            return "redirect:/posts"; // 숫자가 아니면 리스트로
        }
    }

    // 3. 홈 리스트
    @GetMapping("/posts")
    public String list(Model model) {
        model.addAttribute("posts", postService.getHomePosts());
        return "index";
    }

    // 4. 글 등록(POST)
    @PostMapping("/posts")
    public String createPost(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        Integer userId = getCurrentUserIdOrNull();
        if (userId == null) return "redirect:/user/login";

        CategoryEntity categoryEntity = categoryRepository.findByCategoryName(category)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리: " + category));

        // DB 저장
        PostEntity post = postService.create(userId, categoryEntity.getCategoryId(), title, description);
        if (images != null && !images.isEmpty()) {
            postImageService.addImages(post, images);
        }

        // ⭐ 수정 포인트: .html을 붙이지 않고 설정한 @GetMapping 주소로 리다이렉트
        return "redirect:/posts/" + post.getPostId();
    }

    // 유저 정보 헬퍼 메서드 (기존과 동일)
    private String getLoginIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) return null;
        return auth.getName();
    }

    private Integer getCurrentUserIdOrNull() {
        String loginId = getLoginIdOrNull();
        return (loginId == null) ? null : userRepository.findByLoginId(loginId).map(UserEntity::getUserId).orElse(null);
    }

    private String getCurrentNicknameOrNull() {
        String loginId = getLoginIdOrNull();
        return (loginId == null) ? null : userRepository.findByLoginId(loginId).map(UserEntity::getNickname).orElse(null);
    }
}