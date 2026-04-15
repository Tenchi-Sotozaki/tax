package jp.lg.asp.accommodation.controller;

import jp.lg.asp.accommodation.dto.AddressDto;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressSearchApiController {

    // ダミーデータ（DB実装後は Repository に差し替える）
    private static final List<AddressDto> DUMMY_DATA = List.of(
        new AddressDto("A001001", "田中 太郎",   "たなか たろう",   "東京都新宿区西新宿1-1-1", "03-1234-5678"),
        new AddressDto("A001002", "佐藤 花子",   "さとう はなこ",   "東京都渋谷区渋谷2-2-2",   "03-2345-6789"),
        new AddressDto("A001003", "鈴木 一郎",   "すずき いちろう", "東京都千代田区丸の内3-3-3","03-3456-7890"),
        new AddressDto("A001004", "高橋 美咲",   "たかはし みさき", "東京都港区赤坂4-4-4",     "03-4567-8901"),
        new AddressDto("A001005", "山田 健太",   "やまだ けんた",   "東京都中央区銀座5-5-5",   "03-5678-9012")
    );

    /**
     * GET /api/address/search
     * 宛名番号・氏名・住所で部分一致検索（ダミー実装）
     */
    @GetMapping("/search")
    public List<AddressDto> search(
            @RequestParam(required = false) String addressNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address) {

        return DUMMY_DATA.stream()
                .filter(d -> !StringUtils.hasText(addressNumber)
                        || d.getAddressNumber().contains(addressNumber))
                .filter(d -> !StringUtils.hasText(name)
                        || d.getName().contains(name))
                .filter(d -> !StringUtils.hasText(address)
                        || d.getAddress().contains(address))
                .toList();
    }
}
