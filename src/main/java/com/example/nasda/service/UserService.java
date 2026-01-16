package com.example.nasda.service;

import com.example.nasda.domain.UserEntity;
import com.example.nasda.domain.UserRepository;
import com.example.nasda.dto.UserJoinDto;
import com.example.nasda.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Optional<UserEntity> findByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId);
    }

    /**
     * 회원가입 로직
     */
    @Transactional
    public Integer join(UserJoinDto dto) {
        validateDuplicateMember(dto);
        UserEntity userEntity = userMapper.toEntity(dto);
        userEntity.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(userEntity);
        return userEntity.getUserId();
    }

    private void validateDuplicateMember(UserJoinDto dto) {
        if (userRepository.existsByLoginId(dto.getLoginId())) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("이미 등록된 이메일입니다.");
        }
        if (userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }
    }

    /**
     * 프로필 수정 로직
     * [수정] 파라미터 타입을 Integer로 변경하여 엔티티와 맞춤
     */
    @Transactional
    public UserEntity updateProfile(Integer id, String nickname, String email) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setNickname(nickname);
        user.setEmail(email);

        return user; // Dirty Checking으로 자동 저장됨
    }

    /**
     * 계정 탈퇴 로직
     * [수정] 파라미터 타입을 Integer로 변경
     */
    @Transactional
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    /**
     * 닉네임 중복 확인 (Controller 에러 해결용)
     */
    public boolean isLoginIdDuplicate(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    /**
     * 닉네임 중복 확인
     * 컨트롤러에서 호출하는 이름: isNicknameDuplicate
     */
    public boolean isNicknameDuplicate(String nickname) {
        // 이미 존재하면 true (중복), 없으면 false를 반환합니다.
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 이메일 중복 확인
     * 컨트롤러에서 호출하는 이름: isEmailDuplicate
     */
    public boolean isEmailDuplicate(String email) {
        // 이미 존재하면 true (중복), 없으면 false를 반환합니다.
        return userRepository.existsByEmail(email);
    }
}