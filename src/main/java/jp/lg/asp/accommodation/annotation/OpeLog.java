package jp.lg.asp.accommodation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作ログ記録用アノテーション
 * コントローラーメソッドに付与して、操作ログを自動記録する
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpeLog {

	/**
	 * 画面ＩＤ
	 * @return 画面ＩＤ
	 */
	String screenId() default "";

	/**
	 * 操作名
	 * @return 操作名
	 */
	String operation() default "";
}