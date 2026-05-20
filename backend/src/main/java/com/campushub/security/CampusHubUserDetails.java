package com.campushub.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

@Getter
public class CampusHubUserDetails extends org.springframework.security.core.userdetails.User {

    private final Long userId;
    private final String email;

    public CampusHubUserDetails(Long userId, String email, String role,
                                 Collection<? extends GrantedAuthority> authorities) {
        super(email, "", true, true, true, true, authorities);
        this.userId = userId;
        this.email = email;
    }

    public static CampusHubUserDetails fromToken(Long userId, String email, String role) {
        return new CampusHubUserDetails(
                userId, email, role,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
