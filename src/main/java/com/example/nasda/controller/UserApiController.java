package com.example.nasda.controller;

import com.example.nasda.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member") // JS의 /api/member 경로와 일치시킴
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/check-loginId")
    public ResponseEntity<Boolean> checkId(@RequestParam("loginId") String loginId) {
        // 중복이면 true, 사용 가능하면 false 반환
        return ResponseEntity.ok(userService.isLoginIdDuplicate(loginId));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam("nickname") String nickname) {
        return ResponseEntity.ok(userService.isNicknameDuplicate(nickname));
    }
}