package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZeiritsuForm {

	@NotBlank(message = "賦課方式は必須です")
	private String fukaKbn;

	@NotBlank(message = "適用開始時期は必須です")
	@Pattern(regexp = "\\d{6}", message = "適用開始時期はyyyyMM形式で入力してください")
	private String tekiyoStYm;

	@Pattern(regexp = "^$|^\\d{6}$", message = "適用終了時期はyyyyMM形式で入力してください")
	private String tekiyoEdYm;

	@NotBlank(message = "区分は必須です")
	private String taishoKbn;

	@Valid
	private List<ZeiritsuDetailForm> details = new ArrayList<>();

	public ZeiritsuForm() {
		for (int i = 0; i < 5; i++) {
			details.add(new ZeiritsuDetailForm());
		}
	}

	public static Map<String, String> validate(ZeiritsuForm f) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (f.getFukaKbn() == null || f.getFukaKbn().isBlank()) errors.put("fukaKbn", "賦課方式は必須です");
		if (f.getTekiyoStYm() == null || f.getTekiyoStYm().isBlank()) errors.put("tekiyoStYm", "適用開始時期は必須です");
		else if (!f.getTekiyoStYm().matches("\\d{6}")) errors.put("tekiyoStYm", "適用開始時期はyyyyMM形式で入力してください");
		if (f.getTekiyoEdYm() != null && !f.getTekiyoEdYm().isBlank() && !f.getTekiyoEdYm().matches("\\d{6}")) errors.put("tekiyoEdYm", "適用終了時期はyyyyMM形式で入力してください");
		if (f.getTaishoKbn() == null || f.getTaishoKbn().isBlank()) errors.put("taishoKbn", "区分は必須です");
		return errors;
	}
}
