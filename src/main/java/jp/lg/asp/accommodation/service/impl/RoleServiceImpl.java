package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.RoleDetail;
import jp.lg.asp.accommodation.entity.RoleId;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.ScreenRepository;
import jp.lg.asp.accommodation.service.RoleService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

	private final RoleRepository roleRepository;
	private final ScreenRepository screenRepository;

	@Override
	public List<Role> findAllRoles(String jichitaiCd) {
		return roleRepository.findByJichitaiCdWithDetails(jichitaiCd);
	}

	@Override
	public List<Screen> findAllScreens() {
		return screenRepository.findByJichitaiCdOrderByScreenId("01202");
	}

	@Override
	public Role findById(String jichitaiCd, Long roleId) {
		return roleRepository.findById(new RoleId(jichitaiCd, roleId)).orElse(null);
	}

	@Override
	@Transactional
	public void saveRole(RoleForm form, String jichitaiCd, String userId) {
		Role role;
		if (form.getRoleId() != null) {
			role = roleRepository.findById(new RoleId(jichitaiCd, form.getRoleId())).orElseThrow();
			role.setUpdDt(LocalDateTime.now());
			role.setUpdUser(userId);
		} else {
			role = new Role();
			role.setJichitaiCd(jichitaiCd);
			role.setAddUser(userId);
			role.setUpdDt(LocalDateTime.now());
			role.setUpdUser(userId);
		}

		role.setName(form.getName());
		role = roleRepository.save(role);

		// 既存の権限詳細を削除
		if (role.getRoleDetails() != null) {
			role.getRoleDetails().clear();
		} else {
			role.setRoleDetails(new java.util.ArrayList<>());
		}

		// 新しい権限詳細を追加
		if (form.getScreenPermissions() != null) {
			for (Map.Entry<String, Integer> entry : form.getScreenPermissions().entrySet()) {
				if (entry.getValue() != null && entry.getValue() > 0) {
					RoleDetail detail = new RoleDetail();
					detail.setJichitaiCd(jichitaiCd);
					detail.setRoleId(role.getRoleId());
					detail.setScreenId(entry.getKey());
					detail.setPermission(entry.getValue());
					detail.setAddUser(userId);
					detail.setUpdDt(LocalDateTime.now());
					detail.setUpdUser(userId);
					role.getRoleDetails().add(detail);
				}
			}
		}

		roleRepository.save(role);
	}

	@Override
	@Transactional
	public void deleteRole(String jichitaiCd, Long roleId) {
		roleRepository.deleteById(new RoleId(jichitaiCd, roleId));
	}
}
