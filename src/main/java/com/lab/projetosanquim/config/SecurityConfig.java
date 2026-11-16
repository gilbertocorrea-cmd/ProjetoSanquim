package com.lab.projetosanquim.config;

import com.lab.projetosanquim.entity.Role;
import com.lab.projetosanquim.security.JwtAuthenticationFilter;
import com.lab.projetosanquim.security.JwtService;
import com.lab.projetosanquim.security.RestAccessDeniedHandler;
import com.lab.projetosanquim.security.RestAuthenticationEntryPoint;
import com.lab.projetosanquim.security.UsuarioDetailsService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UsuarioDetailsService usuarios, PasswordEncoder passwordEncoder
    ) {
        var provider = new DaoAuthenticationProvider(usuarios);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UsuarioDetailsService usuarios,
            RestAuthenticationEntryPoint entryPoint,
            RestAccessDeniedHandler deniedHandler
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority(Role.ROLE_GESTAO.name())
                        .requestMatchers("/api/diario/**")
                        .hasAnyAuthority(Role.ROLE_PROFESSOR.name(), Role.ROLE_GESTAO.name())
                        .requestMatchers("/api/aluno/**").hasAuthority(Role.ROLE_ALUNO.name())
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarios, entryPoint),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
