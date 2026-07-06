package jp.lg.asp.accommodation.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final ScreenRepository screenRepository;
	private final OperationLogRepository operationLogRepository;
	private final EntityManager em;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	@Transactional(readOnly = true)
	public List<OpeLogViewDto> search(OpeLogViewDto form) {

		List<OperationLog> logList = operationLogRepository.findByConditions(
				jichitaiCd,
				form.getScreenId(),
				form.getSousa(),
				form.getOpeUser(),
				form.getOpeDtFrom(),
				form.getOpeDtTo(),
				form.getParam());

		// screen_id → screen_name マッピング用
		List<Screen> screens = screenRepository.findByJichitaiCdOrderByScreenId(jichitaiCd);

		List<OpeLogViewDto> results = new ArrayList<>();
		for (OperationLog log : logList) {
			OpeLogViewDto dto = new OpeLogViewDto();
			dto.setSeq(log.getSeq());
			dto.setScreenId(log.getScreenId());
			dto.setScreenName(resolveScreenName(screens, log.getScreenId()));
			dto.setSousa(log.getSousa());
			dto.setOpeUser(log.getOpeUser());
			dto.setOpeDt(log.getOpeDt());
			dto.setParam(log.getParam());
			results.add(dto);
		}
		return results;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Screen> findAllScreens() {
		return screenRepository.findByJichitaiCdOrderByScreenId(jichitaiCd);
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
