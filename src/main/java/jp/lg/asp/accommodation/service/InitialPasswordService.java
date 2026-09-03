package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.entity.User;

public interface InitialPasswordService {

    User findUser(String jichitaiCd, String userId);

    void changeInitialPassword(User user, String newPassword);
}
