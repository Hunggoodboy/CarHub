package com.carhub.dto;

import com.carhub.entity.User;
import lombok.Data;

import java.security.Principal;

@Data
public class AppPrincipal implements Principal {
    private final Long userId;
    private final String email;
    private final String loginType;
    private final User.Role role;
    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
