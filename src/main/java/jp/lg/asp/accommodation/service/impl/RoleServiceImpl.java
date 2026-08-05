package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.RoleDetail;
import jp.lg.asp.accommodation.entity.RoleId;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.ScreenRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.RoleService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

	private final RoleRepository roleRepository;
	private final ScreenRepository screenRepository;
	private final UserRepository userRepository;
	private final JichitaiContext jichitaiContext;

	@Override
	public List<Role> findAllRoles(String jichitaiCd) {
		return roleRepository.findByJichitaiCdWithDetails(jichitaiCd);
	}

	@Override
	public List<Screen> findAllScreens() {
		return screenRepository.findByJichitaiCdOrderByScreenId(jichitaiContext.getJichitaiCd());
	}

	@Override
	public Map<String, List<Screen>> findScreensGroupedByKbn() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		// m_screen の登録内容を画面IDで引けるようにする
		Map<String, Screen> registered = new LinkedHashMap<>();
		for (Screen screen : findAllScreens()) {
			registered.put(screen.getScreenId(), screen);
		}

		Map<String, List<Screen>> grouped = new LinkedHashMap<>();

		// メニュー構成の定義順に並べる
		// m_screen に未登録の画面も権限を設定できるよう、定義側の画面名で表示する
		for (Map.Entry<String, Map<String, String>> kbn : ScreenManagement.getScreensByKbn().entrySet()) {
			List<Screen> screens = new ArrayList<>();
			for (Map.Entry<String, String> defined : kbn.getValue().entrySet()) {
				Screen screen = registered.remove(defined.getKey());
				if (screen == null) {
					screen = new Screen();
					screen.setJichitaiCd(jichitaiCd);
					screen.setScreenId(defined.getKey());
					screen.setScreenName(defined.getValue());
				}
				screens.add(screen);
			}
			grouped.put(kbn.getKey(), screens);
		}

		// メニュー構成に定義が無い画面は「その他」にまとめる
		if (!registered.isEmpty()) {
			grouped.computeIfAbsent(ScreenManagement.SCREEN_KBN_OTHER, key -> new ArrayList<>())
					.addAll(registered.values());
		}

		// 画面が1件も無い区分は見出しごと表示しない
		grouped.values().removeIf(List::isEmpty);

		return grouped;
	}

	@Override
	public Role findById(String jichitaiCd, Long roleId) {
		return roleRepository.findByIdWithDetails(jichitaiCd, roleId).orElse(null);
	}

	@Override
	@Transactional
	public void saveRole(RoleForm form, String jichitaiCd, String userId) {
		Role role;
		if (form.getRoleId() != null) {
			role = roleRepository.findByIdWithDetails(jichitaiCd, form.getRoleId()).orElseThrow();
			role.setName(form.getName());

			// 既存のroleDetailsをscreenIdでMap化
			Map<String, RoleDetail> existingMap = role.getRoleDetails() == null ? new HashMap<>()
					: role.getRoleDetails().stream().collect(Collectors.toMap(RoleDetail::getScreenId, d -> d));

			List<RoleDetail> updatedDetails = new ArrayList<>();
			if (form.getScreenPermissions() != null) {
				for (Map.Entry<String, Integer> entry : form.getScreenPermissions().entrySet()) {
					if (entry.getValue() != null && entry.getValue() > 0) {
						RoleDetail detail = existingMap.getOrDefault(entry.getKey(), new RoleDetail());
						detail.setJichitaiCd(jichitaiCd);
						detail.setRoleId(role.getRoleId());
						detail.setScreenId(entry.getKey());
						detail.setPermission(String.valueOf(entry.getValue()));
						updatedDetails.add(detail);
					}
				}
			}
			role.getRoleDetails().clear();
			roleRepository.saveAndFlush(role);
			role.getRoleDetails().addAll(updatedDetails);
			roleRepository.save(role);
		} else {
			role = new Role();
			role.setJichitaiCd(jichitaiCd);
			long nextId = roleRepository.findMaxRoleIdByJichitaiCd(jichitaiCd) + 1;
			role.setRoleId(nextId);
			role.setName(form.getName());
			role.setRoleDetails(new ArrayList<>());
			roleRepository.saveAndFlush(role);

			if (form.getScreenPermissions() != null) {
				for (Map.Entry<String, Integer> entry : form.getScreenPermissions().entrySet()) {
					if (entry.getValue() != null && entry.getValue() > 0) {
						RoleDetail detail = new RoleDetail();
						detail.setJichitaiCd(jichitaiCd);
						detail.setRoleId(role.getRoleId());
						detail.setScreenId(entry.getKey());
						detail.setPermission(String.valueOf(entry.getValue()));
						role.getRoleDetails().add(detail);
					}
				}
			}
			roleRepository.save(role);
		}
	}

	@Override
	public List<User> findAssignedUsers(String jichitaiCd, Long roleId) {
		return userRepository.findAssignedUsers(jichitaiCd, BigDecimal.valueOf(roleId));
	}

	@Override
	@Transactional
	public void resetUsersToDefaultRole(String jichitaiCd, Long roleId, String updUser) {
		List<User> users = userRepository.findByJichitaiCdAndRoleId(jichitaiCd, BigDecimal.valueOf(roleId));
		for (User u : users) {
			u.setRoleId(BigDecimal.TWO);
		}
		userRepository.saveAll(users);
	}

	@Override
	@Transactional
	public void deleteRole(String jichitaiCd, Long roleId) {
		roleRepository.deleteById(new RoleId(jichitaiCd, roleId));
	}
}
