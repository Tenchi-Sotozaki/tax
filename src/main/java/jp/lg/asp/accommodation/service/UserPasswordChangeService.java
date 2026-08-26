package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.entity.User;

public interface UserPasswordChangeService {

    User findUser(String jichitaiCd, String userId);

    void changePassword(User user, String newPassword);
}
