package mbti.mbti_test.Dto;

import lombok.*;
import mbti.mbti_test.domain.MbtiList;
import mbti.mbti_test.domain.Member;
import mbti.mbti_test.domain.Result;
import mbti.mbti_test.domain.WhaleCount;

import java.time.LocalDateTime;

//0803 hayoon
@Data
@Getter @Setter
public class CreateResultDto {

    private Long resultId;
    private LocalDateTime resultDate;
    private MbtiList mbtiList;
    private Member member;
    private WhaleCount whaleCount;

    public CreateResultDto(Result result) {
        this.resultId = result.getId();
        this.resultDate = result.getTestTime();
        this.mbtiList = result.getMbtiList();
        this.member = result.getMember();
        this.whaleCount = result.getWhaleCount();
    }
}
