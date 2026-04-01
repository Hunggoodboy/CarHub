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
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**","/js/**", "/images/**","/car-images/**", "/webjars/**").permitAll()
                        .requestMatchers("/", "/index", "/register", "/login", "/ChatAI","/chat", "/error").permitAll()
                        .requestMatchers("/api/auth/**", "/api/cars/**").permitAll()
                        .requestMatchers("/api/password/**").authenticated()
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers("/customer-view").hasRole("CUSTOMER")
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
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error=true")
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(
                                        new CustomAuthorizationRequestResolver(
                                                clientRegistrationRepository,
                                                "/oauth2/authorization"
                                        )
                                )
                        )
                )
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