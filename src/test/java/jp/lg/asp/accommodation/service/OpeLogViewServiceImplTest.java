package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.OpeLogViewDto;
import jp.lg.asp.accommodation.entity.OperationLog;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.repository.OperationLogRepository;
import jp.lg.asp.accommodation.repository.ScreenRepository;
import jp.lg.asp.accommodation.service.impl.OpeLogViewServiceImpl;

@ExtendWith(MockitoExtension.class)
class OpeLogViewServiceImplTest {

	@Mock
	ScreenRepository screenRepository;
	@Mock
	OperationLogRepository operationLogRepository;
	@Mock
	JichitaiContext jichitaiContext;
	@InjectMocks
	OpeLogViewServiceImpl service;

	private static final String JICHITAI_CD = "011002";

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
	}

	@Test
	void search_mapsLogToDto() {
		OperationLog log = new OperationLog();
		log.setSeq(1L);
		log.setScreenId("SCR001");
		log.setSousa("登録");
		log.setOpeUser("user01");

		Screen screen = new Screen();
		screen.setScreenId("SCR001");
		screen.setScreenName("特別徴収義務者管理");

		when(operationLogRepository.findByConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any()))
				.thenReturn(List.of(log));
		when(screenRepository.findByJichitaiCdOrderByScreenId(JICHITAI_CD)).thenReturn(List.of(screen));

		OpeLogViewDto form = new OpeLogViewDto();
		List<OpeLogViewDto> result = service.search(form);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getScreenId()).isEqualTo("SCR001");
		assertThat(result.get(0).getScreenName()).isEqualTo("特別徴収義務者管理");
		assertThat(result.get(0).getSousa()).isEqualTo("登録");
	}

	@Test
	void search_unknownScreenId_fallsBackToId() {
		OperationLog log = new OperationLog();
		log.setScreenId("UNKNOWN");

		when(operationLogRepository.findByConditions(any(), any(), any(), any(), any(), any()))
				.thenReturn(List.of(log));
		when(screenRepository.findByJichitaiCdOrderByScreenId(JICHITAI_CD)).thenReturn(List.of());

		List<OpeLogViewDto> result = service.search(new OpeLogViewDto());

		assertThat(result.get(0).getScreenName()).isEqualTo("UNKNOWN");
	}

	@Test
	void search_emptyLogs_returnsEmptyList() {
		when(operationLogRepository.findByConditions(any(), any(), any(), any(), any(), any()))
				.thenReturn(List.of());
		when(screenRepository.findByJichitaiCdOrderByScreenId(JICHITAI_CD)).thenReturn(List.of());

		assertThat(service.search(new OpeLogViewDto())).isEmpty();
	}

	@Test
	void findAllScreens_delegatesToRepository() {
		Screen screen = new Screen();
		when(screenRepository.findByJichitaiCdOrderByScreenId(JICHITAI_CD)).thenReturn(List.of(screen));

		assertThat(service.findAllScreens()).hasSize(1);
	}
}
