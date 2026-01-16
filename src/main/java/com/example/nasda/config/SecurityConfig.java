package com.example.nasda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (POST 요청 허용을 위해 필수)
                // SecurityConfig.java 예시
                .authorizeHttpRequests(auth -> auth
                        // 1. 유저 관련 모든 경로 (/user/login, /user/signup, /user/mypage 등) 허용
                        .requestMatchers("/", "/user/**").permitAll()
                        // 2. 게시글 관련 모든 경로 (/posts/create, /posts/view 등) 허용
                        .requestMatchers("/posts/**", "/post/**").permitAll()
                        // 3. 정적 리소스 허용
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/member/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                // ⭐ 중요: 아래 설정을 통해 시큐리티 기본 로그인 로직을 우리가 만든 컨트롤러로 양보합니다.
                .formLogin(form -> form
                        .loginPage("/user/login") // GET 요청만 이 페이지를 띄우게 함
                        // loginProcessingUrl을 설정하지 않거나 다른 주소로 바꿔서
                        // 시큐리티가 POST /user/login을 가로채지 않게 합니다.
                        .loginProcessingUrl("/do-not-use-this-login")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    // ⭐ 이 메서드가 있어야 UserService의 PasswordEncoder 에러가 해결됩니다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}