package com.example.nasda.controller;

import com.example.nasda.domain.UserEntity;
import com.example.nasda.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.nasda.dto.post.PostViewDto;
import com.example.nasda.repository.CommentRepository;
import com.example.nasda.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import com.example.nasda.dto.UserJoinDto;
import com.example.nasda.service.UserService;


import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final LoginService loginService;
    private final UserService userService;
    private final PostService postService;
    private final CommentRepository commentRepository;

    @GetMapping("/login")
    public String loginForm() {
        return "user/login";
    }

    // ✅ 추가 1: 회원가입 화면 GET
    @GetMapping("/signup")
    public String signupForm() {
        return "user/signup";
    }

    // ✅ 추가 2: 마이페이지 화면 GET
    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/user/login";

        Integer userId = loginUser.getUserId();

        // 템플릿이 요구하는 모델 값들
        model.addAttribute("user", loginUser);

        model.addAttribute("postCount", postService.countMyPosts(userId));
        model.addAttribute("commentCount", commentRepository.countByUserId(userId));

        List<PostViewDto> myPosts = postService.getMyRecentPosts(userId, 4);
        model.addAttribute("myPosts", myPosts); // ✅ 절대 null이면 안 됨

        return "user/mypage";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        Model model) {
        try {
            UserEntity loginUser = loginService.login(username, password);

            if (loginUser != null) {
                session.setAttribute("loginUser", loginUser);

                UsernamePasswordAuthenticationToken token =
                        new UsernamePasswordAuthenticationToken(
                                loginUser.getLoginId(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                SecurityContextHolder.getContext().setAuthentication(token);

                session.setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        SecurityContextHolder.getContext()
                );

                return "redirect:/";
            }

            model.addAttribute("errorMessage", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "user/login";

        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "user/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response,
                         HttpSession session) {

        session.invalidate();
        new SecurityContextLogoutHandler().logout(
                request, response, SecurityContextHolder.getContext().getAuthentication()
        );

        return "redirect:/";
    }
    // ✅ 추가: 마이페이지 신고 내역 화면 GET
    @GetMapping("/mypage/reports")
    public String myReportList(HttpSession session,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               Model model) {

        // 1. 세션에서 로그인 유저 정보 가져오기
        UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/user/login";

        // 2. HTML에서 사용할 'dummyReports' 데이터 담기
        // 현재 HTML이 dummyReports를 반복문(th:each)으로 돌리고 있다면 아래 코드가 필수입니다.
        model.addAttribute("dummyReports", List.of("부적절한 언어 사용으로 신고된 내역입니다."));

        // 페이징 처리를 위한 빈 객체 (기존 코드 유지)
        model.addAttribute("paging", org.springframework.data.domain.Page.empty());

        // 3. 유저 정보 전달
        model.addAttribute("user", loginUser);

        return "user/my-reports"; // my-reports.html 파일을 보여줌
    }
    // ✅ 추가: 마이페이지 프로필 업데이트 (비밀번호 확인 포함)
    @PostMapping("/mypage/update")
    public String updateProfile(@RequestParam String nickname,
                                @RequestParam String email,
                                @RequestParam String currentPassword,
                                HttpSession session,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // 1. 세션에서 로그인 유저 정보 가져오기
        UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/user/login";

        try {
            // 2. 비밀번호 검증 (이미 만들어진 loginService.login 활용)
            // 비밀번호가 틀리면 여기서 RuntimeException이 발생하여 catch 블록으로 갑니다.
            loginService.login(loginUser.getLoginId(), currentPassword);

            // 3. 서비스의 updateProfile 호출하여 DB 수정
            userService.updateProfile(loginUser.getUserId(), nickname, email);

            // 4. 세션에 저장된 유저 정보도 최신화 (마이페이지에 바로 반영되도록)
            loginUser.setNickname(nickname);
            loginUser.setEmail(email);
            session.setAttribute("loginUser", loginUser);

            redirectAttributes.addFlashAttribute("message", "정보가 성공적으로 수정되었습니다.");

        } catch (RuntimeException e) {
            // 비밀번호가 틀렸을 경우 메시지와 함께 계정 관리 탭으로 리다이렉트
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            // 에러 발생 시 계정 관리 탭을 유지하기 위해 파라미터 전달 (스크립트 처리용)
            return "redirect:/user/mypage?tab=account";
        }

        return "redirect:/user/mypage";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String loginId,
                         @RequestParam String password,
                         @RequestParam String nickname,
                         @RequestParam String email,
                         Model model) {
        try {
            UserJoinDto dto = new UserJoinDto(loginId, password, nickname, email);
            userService.join(dto);

            return "redirect:/user/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "user/signup";
        }

    }
}
