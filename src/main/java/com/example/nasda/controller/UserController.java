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

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final LoginService loginService;

    @GetMapping("/login")
    public String loginForm() {
        return "user/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        Model model) {
        try {
            UserEntity loginUser = loginService.login(username, password);

            if (loginUser != null) {
                // 1. 일반 세션에 저장 (기존 방식 유지)
                session.setAttribute("loginUser", loginUser);

                // 2. ⭐ 스프링 시큐리티 인증 객체 생성 및 등록
                // loginUser.getLoginId()를 인증 주체(Principal)로 설정합니다.
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                        loginUser.getLoginId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

                // 시큐리티 컨텍스트에 저장
                SecurityContextHolder.getContext().setAuthentication(token);

                // 3. 세션에 시큐리티 컨텍스트 동기화 (이게 있어야 다음 요청에서도 유지됨)
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        SecurityContextHolder.getContext());

                return "redirect:/";
            }
            model.addAttribute("errorMessage", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "user/login";

        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "user/login";
        }
    }
}