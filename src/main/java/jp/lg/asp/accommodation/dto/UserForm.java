package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UserForm {

	public interface OnCreate {}
	public interface OnUpdate {}

	private String originalId;

	@NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "IDは必須です")
	@Size(max = 100)
	private String id;

	private String currentPassword;

	@NotBlank(groups = OnCreate.class, message = "パスワードは必須です")
	@Size(max = 64, message = "パスワードは64文字以内で入力してください")
	private String password;

	private String passwordConfirm;

	@NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "氏名は必須です")
	@Size(max = 200)
	private String name;

	@NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "ふりがなは必須です")
	@Size(max = 200)
	private String nameKana;

	@NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "部署は必須です")
	@Size(max = 200)
	private String busho;

	private BigDecimal roleId;
}
