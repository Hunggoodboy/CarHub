package com.carhub.service.oauth2;


import com.carhub.entity.User;
import com.carhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Oauth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setFullName(name);
            user.setEmail(email);
            user.setUsername(email);

            user.setPassword("");
            user.setRole(User.Role.CUSTOMER);
            return userRepository.save(user);
        });

        return oAuth2User;
    }
}
