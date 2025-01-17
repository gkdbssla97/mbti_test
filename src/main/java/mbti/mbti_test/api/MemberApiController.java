package mbti.mbti_test.api;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import mbti.mbti_test.config.security.access.AccessService;
import mbti.mbti_test.config.security.jwt.JwtTokenProvider;
import mbti.mbti_test.config.security.user.CustomUserDetailService;
import mbti.mbti_test.config.security.user.MemberAdapter;
import mbti.mbti_test.config.security.user.MemberLoginRepository;
import mbti.mbti_test.domain.Address;
import mbti.mbti_test.domain.Member;
import mbti.mbti_test.dto.*;
import mbti.mbti_test.exception.MemberAlreadyExistException;
import mbti.mbti_test.service.MemberService;
import mbti.mbti_test.service.impl.MemberServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

//0803 Hayoon
@RestController
@RequiredArgsConstructor
@Slf4j
public class MemberApiController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final MemberLoginRepository memberLoginRepository;
    private final MemberServiceImpl memberServiceImpl;
    private final CustomUserDetailService customUserDetailService;
    private final StringRedisTemplate redisTemplate;

    private final AccessService accessService;
    private final Long tokenInvalidTime = 1000L * 60 * 60; // 토큰 1시간

    @GetMapping("/api/v2/members")
    public List<CreateMemberDto> memberV2() {
        List<Member> members = memberService.findMembers();

        return members.stream()
                .map(member -> new CreateMemberDto(member))
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    //회원가입
    @PostMapping("/api/v3/join")
    public CreateMemberResponse saveMemberV3(@RequestBody CreateMemberDto createMemberDto)
            throws MemberAlreadyExistException {
        Long memberId = memberServiceImpl.join(createMemberDto);

        log.info("\nmemberId: " + memberId + " memberDtoName: " + createMemberDto.getName());
        return new CreateMemberResponse(memberId, createMemberDto.getName());

//        return memberId != null ?
//                ResponseEntity.ok().body("회원가입을 축하합니다.") :
//                ResponseEntity.badRequest().build();
    }
    /**
     * {
        "account": "sejong123",
        "pwd": "123456",
        "address": {
            "city": "서울시",
            "street": "광진구",
            "zipcode": "세종3로"
        },
        "email": "abcd1234@naver.com",
        "name": "AAA"
        }
     */

    //로그인
    //0808 Hayoon
    //https://jwt.io/ 에서 Encoder -> Decoder(Payload) 확인가능.
    @PostMapping("/api/v3/login")
    public String login(@RequestBody @Valid UserLoginDto userDto) {
        System.out.println(userDto.getAccount() + " " + userDto.getPassword());
        Optional<Member> byAccount = memberLoginRepository.findByAccount(userDto.getAccount());

        if(byAccount.isPresent()) {
            UserDetails userDetails = customUserDetailService.loadUserByUsername(byAccount.get().getAccount());
            if (!passwordEncoder.matches(userDto.getPassword(), userDetails.getPassword())) {
                throw new IllegalArgumentException("잘못된 비밀번호입니다.");
            }

            log.info("\n로그인 성공!");
            return jwtTokenProvider.createToken(byAccount.get().getAccount(), byAccount.get().getRoles(), tokenInvalidTime);
        } else throw new IllegalStateException("계정이 없습니다.");
    }

    //로그인
    //0820 Hayoon
    //https://jwt.io/ 에서 Encoder -> Decoder(Payload) 확인가능.
    //Bearer Token 생성
    @PostMapping("/api/v4/login")
    public ResponseEntity<TokenResponse> loginV4(@RequestBody @Valid UserLoginDto userDto) {
        System.out.println(userDto.getAccount() + " " + userDto.getPassword());
        Member member = memberLoginRepository.findByAccount(userDto.getAccount())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 ACCOUNT 입니다."));
        if (!passwordEncoder.matches(userDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        log.info("\n로그인 성공!");
        String token = jwtTokenProvider.createToken(member.getUsername(), member.getRoles(), tokenInvalidTime);
        return ResponseEntity.ok().body(new TokenResponse(token, "bearer", member.getAccount()));
    }

    //loginV5 생성 access, refresh 토큰 발급 Dto로 반환
    @PostMapping("/api/v5/login")
    public LoginRepositoryDto loginV5(@RequestBody @Valid UserLoginDto userLoginDto) {

        Member member = memberLoginRepository.findByAccount(userLoginDto.getAccount())
                .orElseThrow(() -> new IllegalStateException("가입되지 않은 ACCOUNT 입니다."));
        if(!passwordEncoder.matches(userLoginDto.getPassword(), member.getPassword()))
            throw new IllegalStateException("잘못된 비밀번호입니다.");

        String accessToken = jwtTokenProvider.createAccessToken(member.getAccount(), member.getRoles());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getAccount(), member.getRoles());
        return new LoginRepositoryDto(accessToken, refreshToken);
    }

    //로그아웃
    //0821 Hayoon
    @ApiOperation(value ="logout")
    @ApiResponses({@ApiResponse(code = 204, message = "success")})
    @GetMapping("/api/v4/logout")
    public ResponseEntity<?> logout(@RequestHeader("X-AUTH-TOKEN") String jwt) {
        //String token = jwtTokenProvider
        ValueOperations<String, String> logoutValueOperations = redisTemplate.opsForValue();
        logoutValueOperations.set(jwt, jwt);
        MemberAdapter memberAdapter = (MemberAdapter) jwtTokenProvider.getAuthentication(jwt).getPrincipal();
        log.info("로그아웃 유저 아이디: '{}'", memberAdapter.getUsername());
        return new ResponseEntity<>(new DefaultResponseDto( 200,"로그아웃 되었습니다."), OK);
    }

    //토큰 값을 이용한 로그아웃.
    @ApiOperation(value ="logout")
    @ApiResponses({@ApiResponse(code = 204, message = "success")})
    @GetMapping("/api/v5/logout")
    public ResponseEntity<DefaultResponseDto> logout(@RequestParam("account") String account,
                                                     HttpServletRequest httpServletRequest) {
        String accessToken = httpServletRequest.getHeader("X-AUTH-TOKEN"); //accessToken 값
        accessService.logout(account, accessToken);
        DefaultResponseDto response = new DefaultResponseDto(200, "로그아웃 완료");
        return new ResponseEntity<>(response, OK);
    }

    //회원정보 수정
    //0813 Hayoon
    @PutMapping("/api/v2/members")
    public UpdateMemberResponse updateMemberV2(@RequestHeader("X-AUTH-TOKEN") String token,
                                               @RequestBody @Valid UpdateMemberDto updateMemberDto) {
        String account = jwtTokenProvider.getMemberPk(token);
        Optional<Member> findMember = memberLoginRepository.findByAccount(account);
        memberService.updateMember(findMember, updateMemberDto);

        return new UpdateMemberResponse(findMember.get().getEmail(),
                findMember.get().getAddress(), findMember.get().getAccount(), findMember.get().getPwd(),
                findMember.get().getUpdateDateTime());
    }

    //0823 Hayoon
    //회원 아이디 찾기
    @PostMapping("/api/v1/findAccount")
    public ResponseEntity<String> findAccountByEmail(@RequestBody @Valid EmailDto emailDto) {
        Optional<Member> findMemberByEmail = memberLoginRepository.findByEmail(emailDto.getEmail());
        log.info("이메일: " + findMemberByEmail.get().getAccount());
        //throw new IllegalStateException("이메일이 존재하지 않습니다.");
        return findMemberByEmail.map(member -> new ResponseEntity<>(member.getAccount(), OK)).
                orElseGet(() -> new ResponseEntity<>("이메일이 존재하지 않습니다", NOT_FOUND));
    }

    //0823 Hayoon
    //회원 비밀번호 찾기
    @PostMapping("/api/v1/findPassword")
    public ResponseEntity<String> findPwdByAccount(@RequestBody @Valid ChangePwdDto changePwdDto) throws Exception {
        Optional<Member> findMemberByAccount = memberLoginRepository.findByAccount(changePwdDto.getAccount());
        if (findMemberByAccount.isPresent()) {
            if (changePwdDto.getPassword().equals(changePwdDto.getCheckPassword())) {
                memberService.changePwd(findMemberByAccount, changePwdDto);
                return new ResponseEntity<>(changePwdDto.getPassword(), OK);
            } else {
                //throw new IllegalStateException("새비밀번호와 새비밀번호 확인이 일치하지 않습니다.");
                return new ResponseEntity<>("새비밀번호와 새비밀번호 확인이 일치하지 않습니다.", NOT_FOUND);
            }
        } else {
            //throw new IllegalStateException("계정이 존재하지 않습니다.");
            return new ResponseEntity<>("계정이 존재하지 않습니다.", NOT_FOUND);
        }
    }

    //0810 Hayoon
    //User 외 접근 제한을 걸어둔 리소스에 요청을 보내 결과 Response 확인
    @PostMapping("/api/user/test")
    public Map userResponseTest() {
        Map<String, String> result = new HashMap<>();
        result.put("result", "user ok");
        return result;
    }
    //0810 Hayoon
    //Admin 외 접근 제한을 걸어둔 리소스에 요청을 보내 결과 Response 확인
    @PostMapping("/api/admin/test")
    public Map adminResponseTest() {
        Map<String, String> result = new HashMap<>();
        result.put("result", "admin ok");
        return result;
    }
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class CreateMemberResponse {
        private Long createId;
        private String name;

        public CreateMemberResponse(Long createId, String name) {
            this.createId = createId;
            this.name = name;
        }
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class UpdateMemberResponse {
        private String email;
        private Address address;
        private String account;
        private String pwd;
        private LocalDateTime updateDateTime;

        public UpdateMemberResponse(String email, Address address,
                                    String account, String pwd,
                                    LocalDateTime updateDateTime) {
            this.email = email;
            this.address = address;
            this.account = account;
            this.pwd = pwd;
            this.updateDateTime = updateDateTime;
        }
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class TokenResponse {
        private String accessToken;
        private String tokenType;
        private String account;

        public TokenResponse(String accessToken, String tokenType, String account) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.account = account;
        }
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class DefaultResponseDto {
        private int serverStatus;
        private String msg;

        public DefaultResponseDto(int serverStatus, String msg) {
            this.serverStatus = serverStatus;
            this.msg = msg;
        }
    }

}

