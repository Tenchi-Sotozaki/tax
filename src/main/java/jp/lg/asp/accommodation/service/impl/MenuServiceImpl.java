package jp.lg.asp.accommodation.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.entity.Menu;
import jp.lg.asp.accommodation.repository.MenuRepository;
import jp.lg.asp.accommodation.service.MenuService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class menuServiceImpl implements MenuService{
	
	private final MenuRepository menuRepository;
	
	public List<Menu> findAllOrderByDspOdr(){
		return menuRepository.findAllOrderByDspOdr();
	}
}
