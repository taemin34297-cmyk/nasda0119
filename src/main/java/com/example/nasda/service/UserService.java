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
    private final EmailService emailService;
    private String verificationCode; // 메모리에 잠시 저장 (실무에선 Redis나 세션을 권장)


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
     * 아이디 찾기 로직 (Step 1)
     */
    public String findIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserEntity::getLoginId)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 사용자가 없습니다."));
    }

    /**
     * 이메일 중복 확인
     * 컨트롤러에서 호출하는 이름: isEmailDuplicate
     */
    public boolean isEmailDuplicate(String email) {
        // 이미 존재하면 true (중복), 없으면 false를 반환합니다.
        return userRepository.existsByEmail(email);
    }
    /**
     * 아이디 찾기 후 메일 발송 로직
     */
    public void findAndSendId(String email) {
        // 1. DB에서 이메일로 사용자 찾기
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 사용자가 없습니다."));

        // 2. 메일 발송
        String subject = "[Nasda] 요청하신 아이디 찾기 결과입니다.";
        String text = "안녕하세요. Nasda입니다.\n" +
                "고객님의 아이디는 [" + user.getLoginId() + "] 입니다.";

        emailService.sendMail(email, subject, text);
    }
    /**
     * 비밀번호 찾기 (임시 비밀번호 발급)
     */
    @Transactional
    public void findAndSendPassword(String loginId, String email) {
        // 1. 아이디와 이메일이 모두 일치하는 유저 찾기
        UserEntity user = userRepository.findByLoginIdAndEmail(loginId, email)
                .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 사용자가 없습니다."));

        // 2. 임시 비밀번호 생성 (8자리 랜덤 문자열)
        String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);

        // 3. DB 비밀번호 업데이트 (암호화 필수!)
        user.setPassword(passwordEncoder.encode(tempPassword));
        // @Transactional이 걸려있어 save()를 호출하지 않아도 자동으로 DB에 반영됩니다.

        // 4. 메일 발송
        String subject = "[Nasda] 임시 비밀번호가 발급되었습니다.";
        String text = "안녕하세요. Nasda 관리자입니다.\n\n" +
                "고객님의 임시 비밀번호는 [" + tempPassword + "] 입니다.\n" +
                "로그인 후 반드시 비밀번호를 변경해 주세요.";

        emailService.sendMail(email, subject, text);
    }
    /**
     * 6자리 랜덤 인증번호 생성 및 메일 발송
     */
    public void sendVerificationCode(String email) {
        // 1. 6자리 랜덤 숫자 생성 (100000 ~ 999999)
        verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

        // 2. 메일 내용 작성
        String subject = "[Nasda] 회원가입 인증번호입니다.";
        String text = "안녕하세요. Nasda입니다.\n\n" +
                "인증번호는 [" + verificationCode + "] 입니다.\n" +
                "해당 번호를 인증번호 입력창에 입력해주세요.";

        // 3. 메일 발송
        emailService.sendMail(email, subject, text);
    }

    /**
     * 인증번호 확인
     */
    public boolean checkVerificationCode(String code) {
        return verificationCode != null && verificationCode.equals(code);
    }
}