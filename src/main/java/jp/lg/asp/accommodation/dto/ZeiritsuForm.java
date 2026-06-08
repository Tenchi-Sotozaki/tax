package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZeiritsuForm {

	@NotBlank
	private String fukaKbn;

	@NotBlank
	@Pattern(regexp = "\\d{6}", message = "適用開始時期はyyyyMM形式で入力してください")
	private String tekiyoStYm;

	@Pattern(regexp = "^$|^\\d{6}$", message = "適用終了時期はyyyyMM形式で入力してください")
	private String tekiyoEdYm;

	@NotBlank
	private String taishoKbn;

	@Valid
	private List<ZeiritsuDetailForm> details = new ArrayList<>();

	public ZeiritsuForm() {
		for (int i = 0; i < 5; i++) {
			details.add(new ZeiritsuDetailForm());
		}
	}
}
