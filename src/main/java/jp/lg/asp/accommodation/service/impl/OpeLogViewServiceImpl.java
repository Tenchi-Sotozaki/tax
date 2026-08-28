package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.OpeLogViewDto;
import jp.lg.asp.accommodation.entity.OperationLog;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.repository.OperationLogRepository;
import jp.lg.asp.accommodation.repository.ScreenRepository;
import jp.lg.asp.accommodation.service.OpeLogViewService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpeLogViewServiceImpl implements OpeLogViewService {

	private final ScreenRepository screenRepository;
	private final OperationLogRepository operationLogRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public List<OpeLogViewDto> search(OpeLogViewDto form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		List<OperationLog> logList = operationLogRepository.findByConditions(
				jichitaiCd,
				form.getScreenId(),
				form.getSousa(),
				form.getOpeUser(),
				form.getOpeDtFrom(),
				form.getOpeDtTo());

		// screen_id → screen_name マッピング用
		List<Screen> screens = screenRepository.findAllByOrderByScreenIdAsc();

		List<OpeLogViewDto> results = new ArrayList<>();
		for (OperationLog log : logList) {
			OpeLogViewDto dto = new OpeLogViewDto();
			dto.setSeq(log.getSeq());
			dto.setScreenId(log.getScreenId());
			dto.setScreenName(resolveScreenName(screens, log.getScreenId()));
			dto.setSousa(log.getSousa());
			dto.setOpeUser(log.getOpeUser());
			dto.setOpeDt(log.getOpeDt());
			results.add(dto);
		}
		return results;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Screen> findAllScreens() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return screenRepository.findAllByOrderByScreenIdAsc();
	}

	private String resolveScreenName(List<Screen> screens, String screenId) {
		if (screenId == null)
			return "";
		return screens.stream()
				.filter(s -> screenId.strip().equals(s.getScreenId().strip()))
				.map(Screen::getScreenName)
				.findFirst()
				.orElse(screenId);
	}
}
