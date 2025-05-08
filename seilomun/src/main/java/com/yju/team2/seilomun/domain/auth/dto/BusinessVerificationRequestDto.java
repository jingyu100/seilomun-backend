package com.yju.team2.seilomun.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor  // Jackson 역직렬화용 기본 생성자
@AllArgsConstructor // Builder가 사용할 모든 필드 생성자
@Builder            // Builder 패턴 적용
public class BusinessVerificationRequestDto {

    // 사업자등록번호
    @NotBlank(message = "사업자등록번호는 필수입니다")
    @JsonAlias({"bNo"})
    private String b_no;

    // 개업일자
    @NotBlank(message = "개업일자는 필수입니다")
    @JsonAlias({"startDt"})
    private String start_dt;

    // 대표자명
    @NotBlank(message = "대표자명은 필수입니다")
    @JsonAlias({"pNm"})
    private String p_nm;

    // 공동대표자명
    @JsonAlias({"pNm2"})
    @Builder.Default
    private String p_nm2 = "";

    // 상호
    @JsonAlias({"bNm"})
    @Builder.Default
    private String b_nm = "";

    // 법인등록번호
    @JsonAlias({"corpNo"})
    @Builder.Default
    private String corp_no = "";

    // 업종
    @JsonAlias({"bSector"})
    @Builder.Default
    private String b_sector = "";

    // 업태
    @JsonAlias({"bType"})
    @Builder.Default
    private String b_type = "";

    // 사업장 주소
    @JsonAlias({"bAdr"})
    @Builder.Default
    private String b_adr = "";
}