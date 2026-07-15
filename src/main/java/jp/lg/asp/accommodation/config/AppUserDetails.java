package jp.lg.asp.accommodation.config;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AppUserDetails extends User {

    private final boolean mustChangePassword;

    public AppUserDetails(String username, String password,
                           Collection<? extends GrantedAuthority> authorities,
                           boolean mustChangePassword) {
        super(username, password, authorities);
        this.mustChangePassword = mustChangePassword;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}