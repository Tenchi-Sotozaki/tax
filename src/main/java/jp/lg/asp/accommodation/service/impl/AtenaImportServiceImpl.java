package jp.lg.asp.accommodation.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.AtenaRenkei;
import jp.lg.asp.accommodation.repository.AtenaRenkeiRepository;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.service.AtenaImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtenaImportServiceImpl implements AtenaImportService {

    private final AtenaRepository atenaRepository;
    private final AtenaRenkeiRepository atenaRenkeiRepository;

    @Override
    @Transactional
    public AtenaRenkei importCsv(MultipartFile file, String jichitaiCd, String userId) {
        int shinkiKensu = 0;
        int koshinKensu = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // ヘッダー行スキップ
            String headerLine = reader.readLine();
            if (headerLine == null) throw new RuntimeException("CSVファイルが空です");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 8) continue;

                BigDecimal atenaNo = new BigDecimal(cols[0].trim());
                String name = cols[1].trim();
                String nameKana = cols[2].trim();
                String yubinNo = cols[3].trim();
                String jusho = cols[4].trim();
                String tel1 = cols[5].trim();
                String tel2 = cols.length > 6 ? cols[6].trim() : null;
                String kojinNo = cols.length > 7 ? cols[7].trim() : null;
                String hojinNo = cols.length > 8 ? cols[8].trim() : null;

                AtenaId pk = new AtenaId();
                pk.setJichitaiCd(jichitaiCd);
                pk.setAtenaNo(atenaNo);

                boolean isNew = !atenaRepository.existsById(pk);
                Atena atena = atenaRepository.findById(pk).orElse(new Atena());

                atena.setJichitaiCd(jichitaiCd);
                atena.setAtenaNo(atenaNo);
                atena.setKbn(kojinNo != null && !kojinNo.isBlank() ? "1" : "2");
                atena.setName(name);
                atena.setNameKana(nameKana.isBlank() ? null : nameKana);
                atena.setYubinNo(yubinNo.isBlank() ? null : yubinNo);
                atena.setJusho(jusho.isBlank() ? null : jusho);
                atena.setTel1(tel1.isBlank() ? null : tel1);
                atena.setTel2(tel2 == null || tel2.isBlank() ? null : tel2);
                atena.setKojinNo(kojinNo == null || kojinNo.isBlank() ? null : kojinNo);
                atena.setHojinNo(hojinNo == null || hojinNo.isBlank() ? null : hojinNo);
                atenaRepository.save(atena);

                if (isNew) shinkiKensu++; else koshinKensu++;
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV取込に失敗しました: " + e.getMessage(), e);
        }

        BigDecimal nextSeq = atenaRenkeiRepository.findMaxSeqByJichitaiCd(jichitaiCd).add(BigDecimal.ONE);
        AtenaRenkei renkei = new AtenaRenkei();
        renkei.setJichitaiCd(jichitaiCd);
        renkei.setSeq(nextSeq);
        renkei.setFileName(file.getOriginalFilename());
        renkei.setShoriDt(LocalDateTime.now());
        renkei.setShoriKensu(BigDecimal.valueOf(shinkiKensu + koshinKensu));
        renkei.setShinkiKensu(BigDecimal.valueOf(shinkiKensu));
        renkei.setKoshinKensu(BigDecimal.valueOf(koshinKensu));
        return atenaRenkeiRepository.save(renkei);
    }

    @Override
    public List<AtenaRenkei> findHistory(String jichitaiCd) {
        return atenaRenkeiRepository.findByJichitaiCdOrderBySeqDesc(jichitaiCd);
    }
}
