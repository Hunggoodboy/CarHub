package com.carhub.service;


import com.carhub.entity.User;
import com.carhub.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Oauth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    public OAuth2User loadUser(OAuth2UserRequest request){
        OAuth2User oAuth2User = super.loadUser(request);
        if( userRepository.findByEmail(oAuth2User.getAttribute("email")).isEmpty()) {
            String name = oAuth2User.getAttribute("name");
            String email = oAuth2User.getAttribute("email");
            User user = new User();
            user.setFullName(name);
            user.setEmail(email);
            userRepository.save(user);
        }
        return oAuth2User;
    }
}
