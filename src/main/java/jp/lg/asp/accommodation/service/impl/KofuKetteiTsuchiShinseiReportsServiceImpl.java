package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.entity.RptStatus;
import jp.lg.asp.accommodation.repository.ReportsLogRepository;
import jp.lg.asp.accommodation.repository.RptStatusRepository;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiReportsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;

/**
 * 宿泊税特別徴収事務交付金決定通知書・交付申請書 ReportsService実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KofuKetteiTsuchiShinseiReportsServiceImpl implements KofuKetteiTsuchiShinseiReportsService {

	private final ReportsLogRepository reportsLogRepository;
	private final RptStatusRepository rptStatusRepository;
	private final JichitaiContext jichitaiContext;

	@Override
	public byte[] generatekofuKetteiTsuchiShinseiPdf(KofuKetteiTsuchiShinseiDto dto) {
		checkAuthentication();
		log.debug("PDF生成開始 - 指定番号: {}", dto.getShiteiNo());

		boolean isKettei = dto.isKetteiTsuchi();
		boolean isKofu = dto.isShinsei();

		if (!isKettei && !isKofu) {
			throw new RuntimeException("印刷対象が選択されていません。");
		}

		try {
			System.setProperty("net.sf.jasperreports.default.font.name", "IPAex明朝");
			System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");

			Map<String, Object> parameters = new HashMap<>();
			List<JasperPrint> jasperPrintList = new ArrayList<>();

			// 決定通知書
			if (isKettei) {
				JasperReport report = loadReport("reports/kofuKetteiTsuchijrxml.jrxml");
				jasperPrintList.add(JasperFillManager.fillReport(report, parameters,
						new JRBeanCollectionDataSource(Arrays.asList(dto))));
				saveLog(ReportsConstants.KOFU_KETTEI_TSUCHI, dto.getOperation(), dto.getShiteiNo());
				log.debug("決定通知書のレポート生成完了");
			}

			// 交付申請書
			if (isKofu) {
				JasperReport report = loadReport("reports/kofukinShinsei.jrxml");
				jasperPrintList.add(JasperFillManager.fillReport(report, parameters,
						new JRBeanCollectionDataSource(Arrays.asList(dto))));
				saveLog(ReportsConstants.KOFU_SHINSEI, dto.getOperation(), dto.getShiteiNo());
				log.debug("交付申請書のレポート生成完了");
			}

			// PDF結合出力
			JRPdfExporter exporter = new JRPdfExporter();
			exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrintList));

			try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
				exporter.exportReport();
				return out.toByteArray();
			}

		} catch (Exception e) {
			if (e instanceof AccessDeniedException) {
				throw (AccessDeniedException) e;
			}
			log.error("PDF生成に失敗しました - 指定番号: {}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] generateBulkPdf(List<KofuKetteiTsuchiShinseiDto> dtoList) {
		checkAuthentication();
		if (dtoList == null || dtoList.isEmpty()) {
			throw new RuntimeException("帳票データがありません。");
		}

		try {
			System.setProperty("net.sf.jasperreports.default.font.name", "IPAex明朝");
			System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");

			JasperReport ketteiReport = loadReport("reports/kofuKetteiTsuchijrxml.jrxml");
			JasperReport shinseiReport = loadReport("reports/kofukinShinsei.jrxml");

			List<JasperPrint> jasperPrintList = new ArrayList<>();
			for (KofuKetteiTsuchiShinseiDto dto : dtoList) {
				Map<String, Object> parameters = new HashMap<>();

				if (dto.isKetteiTsuchi()) {
					jasperPrintList.add(JasperFillManager.fillReport(ketteiReport, parameters,
							new JRBeanCollectionDataSource(Arrays.asList(dto))));
					saveLog(ReportsConstants.KOFU_KETTEI_TSUCHI, dto.getOperation(), dto.getShiteiNo());
				}
				if (dto.isShinsei()) {
					jasperPrintList.add(JasperFillManager.fillReport(shinseiReport, parameters,
							new JRBeanCollectionDataSource(Arrays.asList(dto))));
					saveLog(ReportsConstants.KOFU_SHINSEI, dto.getOperation(), dto.getShiteiNo());
				}
			}

			if (jasperPrintList.isEmpty()) {
				throw new RuntimeException("印刷対象がありません。");
			}

			JRPdfExporter exporter = new JRPdfExporter();
			exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrintList));
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
			exporter.exportReport();
			return out.toByteArray();

		} catch (Exception e) {
			if (e instanceof AccessDeniedException) {
				throw (AccessDeniedException) e;
			}
			log.error("一括PDF生成に失敗しました", e);
			throw new RuntimeException("一括PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * 認証情報の検証を行うプライベートメソッド
	 */
	private void checkAuthentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
			throw new AccessDeniedException("認証情報が存在しないため、処理を実行できません。");
		}
	}

	/**
	 * JRXMLファイルを安全に読み込んでコンパイル
	 */
	private JasperReport loadReport(String path) throws Exception {
		ClassPathResource res = new ClassPathResource(path);
		if (!res.exists()) {
			throw new RuntimeException("JRXMLファイルが見つかりません: " + path);
		}
		try (java.io.InputStream inputStream = res.getInputStream()) {
			return JasperCompileManager.compileReport(inputStream);
		}
	}

	private void saveLog(String rptId, String operation, String shiteiNo) {
		try {
			String jichitaiCd = jichitaiContext.getJichitaiCd();

			// 帳票ログ
			ReportsLog entity = new ReportsLog();
			entity.setJichitaiCd(jichitaiCd);
			entity.setSeq(reportsLogRepository.findNextSeq(jichitaiCd));
			entity.setRptId(rptId);
			entity.setSousa(operation);
			entity.setShiteiNo(shiteiNo);
			entity.setOpeUser(getCurrentUserId());
			entity.setOpeDt(LocalDateTime.now());

			reportsLogRepository.save(entity);

			// 状況ステータス
			Optional<RptStatus> rptStsOp = rptStatusRepository
					.findByJichitaiCdAndShiteiNoAndRptId(jichitaiCd, shiteiNo, rptId);
			RptStatus rptStsEntity = rptStsOp.orElse(new RptStatus());
			rptStsEntity.setJichitaiCd(jichitaiCd);
			rptStsEntity.setShiteiNo(shiteiNo);
			rptStsEntity.setRptId(rptId);
			rptStsEntity.setCreateDt(LocalDateTime.now());

			rptStatusRepository.save(rptStsEntity);

		} catch (Exception e) {
			log.warn("帳票ログの保存に失敗しました", e);
		}
	}

	private String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null ? authentication.getName() : "anonymous";
	}
}