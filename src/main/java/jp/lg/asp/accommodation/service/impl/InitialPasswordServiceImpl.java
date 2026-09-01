package jp.lg.asp.accommodation.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.InitialPasswordService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InitialPasswordServiceImpl implements InitialPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public User findUser(String jichitaiCd, String userId) {
        UserId pk = new UserId();
        pk.setJichitaiCd(jichitaiCd);
        pk.setId(userId);
        return userRepository.findById(pk)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
    }

    @Override
    @Transactional
    public void changeInitialPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setInitialPasswordFlg("0");
        userRepository.save(user);
    }
}
