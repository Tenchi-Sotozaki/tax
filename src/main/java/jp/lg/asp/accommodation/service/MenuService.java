package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.entity.Menu;

public interface MenuService {
	List<Menu> findAllOrderByDspOdr();
}
