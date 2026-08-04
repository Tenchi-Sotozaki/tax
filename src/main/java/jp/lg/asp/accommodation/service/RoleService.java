package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.User;

import java.util.List;

public interface RoleService {
    
    List<Role> findAllRoles(String jichitaiCd);
    
    List<Screen> findAllScreens();

    /** 画面を画面区分ごとにまとめて取得する（キーが区分の表示名） */
    java.util.Map<String, List<Screen>> findScreensGroupedByKbn();
    
    Role findById(String jichitaiCd, Long roleId);
    
    void saveRole(RoleForm form, String jichitaiCd, String userId);
    
    void deleteRole(String jichitaiCd, Long roleId);

    List<User> findAssignedUsers(String jichitaiCd, Long roleId);

    void resetUsersToDefaultRole(String jichitaiCd, Long roleId, String updUser);
}