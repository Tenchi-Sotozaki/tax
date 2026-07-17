package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UserForm {

	public interface OnCreate {
	}

	public interface OnUpdate {
	}

	private String originalId;

	@NotBlank(groups = { OnCreate.class, OnUpdate.class }, message = "IDは必須です")
	@Size(max = 100)
	private String id;

	@NotBlank(groups = { OnCreate.class, OnUpdate.class }, message = "氏名は必須です")
	@Size(max = 200)
	private String name;

	@NotBlank(groups = { OnCreate.class, OnUpdate.class }, message = "ふりがなは必須です")
	@Size(max = 200)
	private String nameKana;

	@NotBlank(groups = { OnCreate.class, OnUpdate.class }, message = "部署は必須です")
	@Size(max = 200)
	private String busho;

	@NotBlank(groups = { OnCreate.class, OnUpdate.class }, message = "権限は必須です")
	private BigDecimal roleId;

	private String currentPassword;

	@NotBlank(groups = OnCreate.class, message = "パスワードは必須です")
	@Size(max = 64, message = "パスワードは64文字以内で入力してください")
	private String password;

	private String passwordConfirm;

	public static Map<String, String> validate(UserForm f, boolean isCreate) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (f.getId() == null || f.getId().isBlank())
			errors.put("id", "IDは必須です");
		if (f.getName() == null || f.getName().isBlank())
			errors.put("name", "氏名は必須です");
		if (f.getNameKana() == null || f.getNameKana().isBlank())
			errors.put("nameKana", "ふりがなは必須です");
		if (f.getBusho() == null || f.getBusho().isBlank())
			errors.put("busho", "部署は必須です");
		if (f.getRoleId() == null)
			errors.put("RoleId", "権限は必須です");
		if (isCreate && (f.getPassword() == null || f.getPassword().isBlank()))
			errors.put("password", "パスワードは必須です");
		return errors;
	}
}
