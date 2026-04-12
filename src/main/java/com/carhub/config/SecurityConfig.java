package com.carhub.config;

import com.carhub.service.oauth2.Oauth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final Oauth2UserService oauth2UserService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    private static final String[] PUBLIC_RESOURCES = {
            "/css/**", "/js/**", "/images/**", "/car-images/**", "/car_images/**",
            "/car_images_sub/**", "/webjars/**"
    };

    private static final String[] PUBLIC_URLS = {
            "/", "/index", "/register", "/login", "/forgot-password", "/ChatAI", "/chat", "/error","/static/**",
            "/api/auth/**", "/api/password/**", "/api/users/check-username", "/api/users/check-email", "/upload/**"
    };

    private static final String[] AUTHENTICATED_URLS = {
            "/api/cars/purchased", // Phải đặt riêng ra đây để check trước
            "/api/users/**", "/api/users/me/profile", "/api/users/me/change-password",
            "/api/favorites/**", "/my-profile", "/customer-view", "/car/save"
    };

    private static final String[] ADMIN_URLS = {
            "/api/users/**", "/admin/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_RESOURCES).permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        .requestMatchers(AUTHENTICATED_URLS).authenticated()
                        .requestMatchers("/api/cars/**").permitAll()

                        .requestMatchers(ADMIN_URLS).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            request.getSession().setAttribute("justLoggedIn", true);

                            var savedRequest = new org.springframework.security.web.savedrequest.HttpSessionRequestCache()
                            .getRequest(request, response);

                            if (savedRequest != null) {
                               response.sendRedirect(savedRequest.getRedirectUrl());
                            } else {
                                response.sendRedirect("/");
                            }

                            })
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(info -> info.userService(oauth2UserService))
                        .defaultSuccessUrl("/", false))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}