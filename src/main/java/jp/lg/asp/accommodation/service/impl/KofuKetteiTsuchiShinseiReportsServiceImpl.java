package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
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

	@Override
	public byte[] generatekofuKetteiTsuchiShinseiPdf(KofuKetteiTsuchiShinseiDto dto) {
		try {
			log.debug("PDF生成開始 - 指定番号: {}", dto.getShiteiNo());
			log.debug("申告納入金額: {}", dto.getNonyugaku());
			log.debug("交付申請額: {}", dto.getKofugaku());

			// 印刷対象選択フラグ
			boolean isKettei = dto.isKetteiTsuchi(); // 決定通知書
			boolean isKofu = dto.isShinsei(); // 交付申請書

			// 印刷対象無し
			if (!isKettei && !isKofu) {
				throw new RuntimeException("印刷対象が選択されていません。");
			}

			// JasperReportsのIPAex明朝フォント設定
			System.setProperty("net.sf.jasperreports.default.font.name", "IPAex明朝");
			System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");

			// パラメータ設定
			Map<String, Object> parameters = new HashMap<>();
			
			// 印刷対象のJasperPrintを格納するリスト
			List<JasperPrint> jasperPrintList = new ArrayList<>();

			// 決定通知書の生成
			if (isKettei) {
				ClassPathResource ketteiResource = new ClassPathResource("reports/kofuKetteiTsuchijrxml.jrxml");
				if (!ketteiResource.exists()) {
					throw new RuntimeException("JRXMLファイルが見つかりません: reports/kofuKetteiTsuchijrxml.jrxml");
				}

				JRBeanCollectionDataSource ketteiDataSource = new JRBeanCollectionDataSource(Arrays.asList(dto));
				JasperReport ketteiReport = JasperCompileManager.compileReport(ketteiResource.getInputStream());
				JasperPrint ketteiPrint = JasperFillManager.fillReport(ketteiReport, parameters, ketteiDataSource);
				
				jasperPrintList.add(ketteiPrint);
				log.debug("決定通知書のレポート生成完了");
			}

			// 交付申請書の生成
			if (isKofu) {
				ClassPathResource kofuResource = new ClassPathResource("reports/kofukinShinsei.jrxml");
				if (!kofuResource.exists()) {
					throw new RuntimeException("JRXMLファイルが見つかりません: reports/kofukinShinsei.jrxml");
				}

				JRBeanCollectionDataSource kofuDataSource = new JRBeanCollectionDataSource(Arrays.asList(dto));
				JasperReport kofuReport = JasperCompileManager.compileReport(kofuResource.getInputStream());
				JasperPrint kofuPrint = JasperFillManager.fillReport(kofuReport, parameters, kofuDataSource);
				
				jasperPrintList.add(kofuPrint);
				log.debug("交付申請書のレポート生成完了");
			}

			// 複数のJasperPrintを1つのPDFバイト配列に結合して出力
			JRPdfExporter exporter = new JRPdfExporter();
			exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrintList));
			
			// バイト配列出力先の作成
			java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
			
			// エグスポーターに出力先を設定
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(byteArrayOutputStream));
			
			// レポートをPDF化
			exporter.exportReport();
			
			return byteArrayOutputStream.toByteArray();

		} catch (Exception e) {
			log.error("PDF生成に失敗しました - 指定番号: {}", dto != null ? dto.getShiteiNo() : "null", e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] generateBulkPdf(List<KofuKetteiTsuchiShinseiDto> dtoList) {
		if (dtoList == null || dtoList.isEmpty()) {
			throw new RuntimeException("帳票データがありません。");
		}
		try {
			System.setProperty("net.sf.jasperreports.default.font.name", "IPAex明朝");
			System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");

			List<JasperPrint> jasperPrintList = new ArrayList<>();
			for (KofuKetteiTsuchiShinseiDto dto : dtoList) {
				Map<String, Object> parameters = new HashMap<>();
				if (dto.isKetteiTsuchi()) {
					ClassPathResource res = new ClassPathResource("reports/kofuKetteiTsuchijrxml.jrxml");
					if (!res.exists()) throw new RuntimeException("JRXMLファイルが見つかりません: reports/kofuKetteiTsuchijrxml.jrxml");
					JasperReport report = JasperCompileManager.compileReport(res.getInputStream());
					jasperPrintList.add(JasperFillManager.fillReport(report, parameters, new JRBeanCollectionDataSource(Arrays.asList(dto))));
				}
				if (dto.isShinsei()) {
					ClassPathResource res = new ClassPathResource("reports/kofukinShinsei.jrxml");
					if (!res.exists()) throw new RuntimeException("JRXMLファイルが見つかりません: reports/kofukinShinsei.jrxml");
					JasperReport report = JasperCompileManager.compileReport(res.getInputStream());
					jasperPrintList.add(JasperFillManager.fillReport(report, parameters, new JRBeanCollectionDataSource(Arrays.asList(dto))));
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
			log.error("一括PDF生成に失敗しました", e);
			throw new RuntimeException("一括PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}
}