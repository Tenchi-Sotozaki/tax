package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.EltaxConstants;
import jp.lg.asp.accommodation.dto.EltaxRenkeiDto;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.service.EltaxRenkeiService;
import jp.lg.asp.accommodation.service.NokigenService;

@ExtendWith(MockitoExtension.class)
class EltaxRenkeiControllerTest {

    @Mock EltaxRenkeiService eltaxRenkeiService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock NokigenService nokigenService;

    @InjectMocks EltaxRenkeiController controller;

    // -------------------------------------------------------------------------
    // index
    // -------------------------------------------------------------------------

    // No.1: 納入期限登録済みの場合に初期表示が成功する
    @Test
    void index_納入期限登録済みの場合に初期表示が成功する() {
        Model model = new ExtendedModelMap();
        when(nokigenService.findAll()).thenReturn(List.of(new Nokigen()));
        when(eltaxRenkeiService.findAll()).thenReturn(List.of());

        String view = controller.index(model);

        verify(accessChecker).checkAccess(ScreenManagement.ELTAX_RENKEI);
        assertThat(view).isEqualTo("eltaxRenkei/eltaxRenkei");
    }

    // No.2: 納入期限が未登録の場合にエラーメッセージを設定する
    @Test
    void index_納入期限未登録の場合にエラーメッセージを設定する() {
        Model model = new ExtendedModelMap();
        when(nokigenService.findAll()).thenReturn(List.of());
        when(eltaxRenkeiService.findAll()).thenReturn(List.of());

        controller.index(model);

        assertThat(model.asMap().get("errorMessage")).isEqualTo("納入期限が登録されていません。");
        assertThat(model.asMap()).containsKey("eltaxRenkeiList");
    }

    // No.3: 納入期限が登録済みの場合はエラーメッセージを設定しない
    @Test
    void index_納入期限登録済みの場合はエラーメッセージを設定しない() {
        Model model = new ExtendedModelMap();
        when(nokigenService.findAll()).thenReturn(List.of(new Nokigen()));
        when(eltaxRenkeiService.findAll()).thenReturn(List.of());

        controller.index(model);

        assertThat(model.asMap()).doesNotContainKey("errorMessage");
    }

    // No.4: eLTAX連携情報一覧をモデルへ設定する
    @Test
    void index_eLTAX連携情報一覧をモデルへ設定する() {
        Model model = new ExtendedModelMap();
        List<EltaxRenkeiDto> list = List.of();
        when(nokigenService.findAll()).thenReturn(List.of(new Nokigen()));
        when(eltaxRenkeiService.findAll()).thenReturn(list);

        controller.index(model);

        verify(eltaxRenkeiService).findAll();
        assertThat(model.asMap().get("eltaxRenkeiList")).isSameAs(list);
    }

    // No.5: 種別名称マップをモデルへ設定する
    @Test
    void index_種別名称マップをモデルへ設定する() {
        Model model = new ExtendedModelMap();
        when(nokigenService.findAll()).thenReturn(List.of(new Nokigen()));
        when(eltaxRenkeiService.findAll()).thenReturn(List.of());

        controller.index(model);

        assertThat(model.asMap().get("shubetsuNameMap")).isEqualTo(EltaxConstants.SHUBETSU_NAME_MAP);
    }

    // No.6: アクセス権限がない場合は例外をそのまま送出する
    @Test
    void index_アクセス権限がない場合は例外をそのまま送出する() {
        Model model = new ExtendedModelMap();
        doThrow(new RuntimeException("AccessDenied")).when(accessChecker).checkAccess(anyString());

        assertThatThrownBy(() -> controller.index(model))
                .isInstanceOf(RuntimeException.class);
        verify(nokigenService, never()).findAll();
        verify(eltaxRenkeiService, never()).findAll();
    }

    // No.7: 納入期限一覧の取得で例外が発生した場合は例外をそのまま送出する
    @Test
    void index_納入期限取得で例外が発生した場合は例外をそのまま送出する() {
        Model model = new ExtendedModelMap();
        when(nokigenService.findAll()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> controller.index(model))
                .isInstanceOf(RuntimeException.class);
        verify(eltaxRenkeiService, never()).findAll();
    }

    // No.8: eLTAX連携情報一覧の取得で例外が発生した場合は例外をそのまま送出する
    @Test
    void index_連携情報取得で例外が発生した場合は例外をそのまま送出する() {
        Model model = new ExtendedModelMap();
        when(nokigenService.findAll()).thenReturn(List.of(new Nokigen()));
        when(eltaxRenkeiService.findAll()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> controller.index(model))
                .isInstanceOf(RuntimeException.class);
        assertThat(model.asMap()).doesNotContainKey("eltaxRenkeiList");
        assertThat(model.asMap()).doesNotContainKey("shubetsuNameMap");
    }

    // -------------------------------------------------------------------------
    // download
    // -------------------------------------------------------------------------

    // No.14: ログファイルを正常にダウンロードできる
    @Test
    void download_ログファイルを正常にダウンロードできる() {
        EltaxRenkei entity = new EltaxRenkei();
        entity.setFileName("連携結果.csv");
        entity.setLog(new byte[]{0x01, 0x02, 0x03});
        when(eltaxRenkeiService.findBySeq(BigDecimal.ONE)).thenReturn(entity);

        ResponseEntity<byte[]> response = controller.download(BigDecimal.ONE);

        verify(accessChecker).checkAccess(ScreenManagement.ELTAX_RENKEI);
        verify(eltaxRenkeiService).findBySeq(BigDecimal.ONE);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new byte[]{0x01, 0x02, 0x03});
    }

    // No.15: ダウンロード時に添付ファイル名をUTF-8で設定する
    @Test
    void download_添付ファイル名をUTF8で設定する() {
        String fileName = "eLTAX連携結果_日本語.csv";
        EltaxRenkei entity = new EltaxRenkei();
        entity.setFileName(fileName);
        entity.setLog(new byte[]{0x01});
        when(eltaxRenkeiService.findBySeq(BigDecimal.ONE)).thenReturn(entity);

        ResponseEntity<byte[]> response = controller.download(BigDecimal.ONE);

        ContentDisposition cd = response.getHeaders().getContentDisposition();
        assertThat(cd.isAttachment()).isTrue();
        assertThat(cd.getFilename()).isEqualTo(fileName);
    }

    // No.16: ダウンロード時のContent-Typeをバイナリ形式に設定する
    @Test
    void download_ContentTypeをバイナリ形式に設定する() {
        EltaxRenkei entity = new EltaxRenkei();
        entity.setFileName("result.dat");
        entity.setLog(new byte[]{0x01});
        when(eltaxRenkeiService.findBySeq(BigDecimal.ONE)).thenReturn(entity);

        ResponseEntity<byte[]> response = controller.download(BigDecimal.ONE);

        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/octet-stream");
    }

    // No.17: 対象データが存在しない場合は404を返す
    @Test
    void download_対象データが存在しない場合は404を返す() {
        when(eltaxRenkeiService.findBySeq(new BigDecimal("999"))).thenReturn(null);

        ResponseEntity<byte[]> response = controller.download(new BigDecimal("999"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }

    // No.18: ログデータがnullの場合は404を返す
    @Test
    void download_ログデータがnullの場合は404を返す() {
        EltaxRenkei entity = new EltaxRenkei();
        entity.setFileName("test.csv");
        entity.setLog(null);
        when(eltaxRenkeiService.findBySeq(BigDecimal.ONE)).thenReturn(entity);

        ResponseEntity<byte[]> response = controller.download(BigDecimal.ONE);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }

    // No.19: ログデータが空配列の場合は空ファイルを返す
    @Test
    void download_ログデータが空配列の場合は空ファイルを返す() {
        EltaxRenkei entity = new EltaxRenkei();
        entity.setFileName("empty.dat");
        entity.setLog(new byte[]{});
        when(eltaxRenkeiService.findBySeq(BigDecimal.ONE)).thenReturn(entity);

        ResponseEntity<byte[]> response = controller.download(BigDecimal.ONE);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    // No.20: アクセス権限がない場合は例外をそのまま送出する
    @Test
    void download_アクセス権限がない場合は例外をそのまま送出する() {
        doThrow(new RuntimeException("AccessDenied")).when(accessChecker).checkAccess(anyString());

        assertThatThrownBy(() -> controller.download(BigDecimal.ONE))
                .isInstanceOf(RuntimeException.class);
        verify(eltaxRenkeiService, never()).findBySeq(any());
    }

    // No.21: ダウンロード対象の取得で例外が発生した場合は例外をそのまま送出する
    @Test
    void download_対象取得で例外が発生した場合は例外をそのまま送出する() {
        when(eltaxRenkeiService.findBySeq(BigDecimal.ONE)).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> controller.download(BigDecimal.ONE))
                .isInstanceOf(RuntimeException.class);
    }
}
