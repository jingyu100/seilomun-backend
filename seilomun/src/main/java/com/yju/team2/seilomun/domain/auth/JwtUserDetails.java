package com.yju.team2.seilomun.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserDetails implements UserDetails {

    private Long id;
    private String email;
    private String username;
    private String userType;

    // 판매자용 생성자
    public static JwtUserDetails fromSeller(Long sellerId, String email, String storeName) {
        JwtUserDetails details = new JwtUserDetails(sellerId, email, storeName, "SELLER");
        return details;
    }

    // 고객용 생성자
    public static JwtUserDetails fromCustomer(Long customerId, String email, String userName) {
        JwtUserDetails details = new JwtUserDetails(customerId, email, userName, "CUSTOMER");
        return details;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userType));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // 판매자인지 확인
    public boolean isSeller() {
        return "SELLER".equals(userType);
    }

    // 고객인지 확인
    public boolean isCustomer() {
        return "CUSTOMER".equals(userType);
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
