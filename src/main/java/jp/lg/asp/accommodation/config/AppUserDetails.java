package jp.lg.asp.accommodation.config;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AppUserDetails extends User {
	
	private static final long serialVersionUID = 1L;

    private final boolean mustChangePassword;
    private String displayName;

    public AppUserDetails(String username, String password,
                           Collection<? extends GrantedAuthority> authorities,
                           boolean mustChangePassword) {
        super(username, password, authorities);
        this.mustChangePassword = mustChangePassword;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
