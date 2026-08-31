package jp.lg.asp.accommodation.service;

import java.util.List;
import java.util.Set;

import jp.lg.asp.accommodation.dto.MenuDto;

public interface GlobalModelService {

    Set<String> getAccessibleScreens(String jichitaiCd, String userId);

    List<MenuDto> buildSideMenuTree(String jichitaiCd, Set<String> screens);
}
