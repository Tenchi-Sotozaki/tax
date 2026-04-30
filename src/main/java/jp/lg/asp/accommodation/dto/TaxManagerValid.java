package jp.lg.asp.accommodation.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TaxManagerValidator.class) // 後述するロジッククラスを指定
@Documented
public @interface TaxManagerValid {
    String message() default "入力内容に誤りがあります";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}