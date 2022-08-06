package mbti.mbti_test.config.security;

import lombok.RequiredArgsConstructor;
import mbti.mbti_test.config.security.user.MemberLoginRepository;
import mbti.mbti_test.domain.Member;
import mbti.mbti_test.domain.MemberStatus;
import mbti.mbti_test.dto.CreateMemberDto;
import mbti.mbti_test.dto.UserDto;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@RequiredArgsConstructor
@Service
public class UserService {

    private final MemberLoginRepository memberLoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    //회원가입
    public Long join(CreateMemberDto createMemberDto) {
        Long memberId = getRole_user(createMemberDto);
        return memberId;
    }

    public Long getRole_user(CreateMemberDto createMemberDto) {

        return memberLoginRepository.save(Member.builder()
                        .account(createMemberDto.getAccount())
                        .pwd(createMemberDto.getPwd())
                        .address(createMemberDto.getAddress())
                        .email(createMemberDto.getEmail())
                        .name(createMemberDto.getName())
                        .createDateTime(createMemberDto.getCreateDateTime())
                        .roles(Collections.singletonList("ROLE_USER"))
                        .build())
                .getId();
    }
}
