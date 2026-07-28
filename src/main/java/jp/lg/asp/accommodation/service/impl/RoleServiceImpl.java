package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.RoleDetail;
import jp.lg.asp.accommodation.entity.RoleId;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
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
	public List<User> findAllUsers(String jichitaiCd) {
		return userRepository.findByJichitaiCdOrderById(jichitaiCd);
	}

	@Override
	@Transactional
	public void updateUserRole(String jichitaiCd, Long roleId, List<String> userIds, String updUser) {
		// 現在このrole_idが付与されているユーザーのrole_idを0にリセット
		List<User> currentUsers = userRepository.findByJichitaiCdAndRoleId(jichitaiCd, BigDecimal.valueOf(roleId));
		for (User u : currentUsers) {
			u.setRoleId(BigDecimal.ZERO);
		}
		userRepository.saveAll(currentUsers);

		if (userIds != null) {
			for (String userId : userIds) {
				UserId pk = new UserId();
				pk.setJichitaiCd(jichitaiCd);
				pk.setId(userId);
				User u = userRepository.findById(pk).orElse(null);
				if (u != null) {
					u.setRoleId(BigDecimal.valueOf(roleId));
					userRepository.save(u);
				}
			}
		}
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
