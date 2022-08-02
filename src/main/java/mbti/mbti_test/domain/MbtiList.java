package mbti.mbti_test.domain;

public enum MbtiList {

    INTJ("일각고래"),
    INTP("상괭이"),
    ENTJ("고양이고래"),
    ENTP("병코돌고래"),
    INFJ("대왕고래"),
    INFP("북극고래"),
    ENFJ("남방큰돌고래"),
    ENFP("벨루가"),
    ISTJ("민부리고래"),
    ISFJ("밍크고래"),
    ESTJ("남방참고래"),
    ISTP("귀신고래"),
    ISFP("항유고래"),
    ESTP("낫돌고래"),
    ESFP("혹등고래");

    private String whaleName;

    MbtiList(String whaleName) {
        this.whaleName = whaleName;
    }

    public String whaleNameMethod() { return this.whaleName; }
}
