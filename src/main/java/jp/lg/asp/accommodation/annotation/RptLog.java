package jp.lg.asp.accommodation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 帳票ログ記録用アノテーション
 * コントローラーメソッドに付与して、帳票ログを自動記録する
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RptLog {

	/**
	 * 帳票ＩＤ
	 * @return 帳票ＩＤ
	 */
	String rptId() default "";

	/**
	 * 操作名
	 * @return 操作名
	 */
	String operation() default "";

	/**
	 * 指定番号（SpEL式で引数を参照可能。例: "#dto.shiteiNo"）
	 * @return 指定番号
	 */
	String shiteiNo() default "";
}
