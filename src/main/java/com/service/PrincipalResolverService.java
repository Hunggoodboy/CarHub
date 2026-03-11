package com.service;

import com.dto.AppPrincipal;
import com.entity.User;
import com.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor

public class PrincipalResolverService implements ApplicationRunner {
    private final UserRepository userRepository;

    private final Map<String, AppPrincipal> principalCache = new HashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("PrincipalResolverService initialized");
    }

    public AppPrincipal getPrincipal(Authentication auth) {
        if(auth.getPrincipal() instanceof OAuth2User oAuth2User){
            String emailOfUser = oAuth2User.getAttribute("email");
            User.Role role = userRepository.findByEmail(emailOfUser).orElseThrow(() -> new RuntimeException("User not found")).getRole();
            return principalCache.computeIfAbsent(emailOfUser, val -> {
                    return new AppPrincipal(userRepository.findIdByEmail(emailOfUser).orElseThrow(() -> new RuntimeException("Can't find this user")), emailOfUser, "OAuth2User", role);
            });
        }
        else if(auth.getPrincipal() instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            Long id = userRepository.findIdByUsername(username).orElseThrow(() -> new RuntimeException("Can't find this user"));
            User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Can't find"));
            return principalCache.computeIfAbsent(user.getEmail(), val -> {
                return new AppPrincipal(id, user.getEmail(), "UserDetails", user.getRole());
            });
        }
        throw new IllegalArgumentException("Unknown principal type");
    }
    public void evict(String key) {
        principalCache.remove(key);
    }
}
