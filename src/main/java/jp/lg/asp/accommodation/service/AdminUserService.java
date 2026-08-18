package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.dto.UserForm;
import jp.lg.asp.accommodation.dto.UserSearchForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.User;

public interface AdminUserService {

    Page<User> search(UserSearchForm form);

    List<User> searchAll(UserSearchForm form);

    List<Role> selectableRoles(String jichitaiCd, BigDecimal currentRoleId);

    User findById(String id);

    boolean existsActiveUser(String id);

    void register(UserForm form);

    void update(String id, UserForm form);

    void delete(String id);

    boolean isLoginUser(String id);
}