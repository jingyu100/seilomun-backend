package com.yju.team2.seilomun.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PasswordValidator.class) // Validator 클래스 지정
@Target({ElementType.FIELD, ElementType.PARAMETER}) // 필드와 메서드 파라미터에서 사용 가능
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 유지
public @interface ValidPassword {

    String message() default "비밀번호는 8~20자 사이여야 하며, 공백일 수 없습니다."; // 기본 메시지

    Class<?>[] groups() default {}; // 그룹 지정

    Class<? extends Payload>[] payload() default {}; // 추가 정보 전달
}
