package mbti.mbti_test.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangePwdDto {

    private String account;
    private String password;
    private String checkPassword;
}
