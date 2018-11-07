package com.chat.toktalk.config;

import com.chat.toktalk.config.handler.Oauth2AuthenticationSuccessHandler;
import com.chat.toktalk.security.TokTalkUserDetailsService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class WebApplicationSecurityConfig extends WebSecurityConfigurerAdapter {

    private final TokTalkUserDetailsService tokTalkUserDetailsService;
    private final Oauth2AuthenticationSuccessHandler successHandler;
    private final DataSource dataSource;
    private static final String REMEMBER_ME_KEY = "cofig-rmkey-BTg2jlnBOQ3lfS5Og5qmFbcfjMl79jfswlaG";

    public WebApplicationSecurityConfig(DataSource dataSource, TokTalkUserDetailsService tokTalkUserDetailsService,Oauth2AuthenticationSuccessHandler successHandler) {
        this.dataSource = dataSource;
        this.tokTalkUserDetailsService = tokTalkUserDetailsService;
        this.successHandler = successHandler;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .requestMatchers(new AntPathRequestMatcher("/**.html"))
                .requestMatchers(new AntPathRequestMatcher("/static/**"))
                .requestMatchers(new AntPathRequestMatcher("/public/**"));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        http
                .authorizeRequests()
                    .antMatchers("/identity/**").permitAll()
                    .antMatchers("/users/login").permitAll()
                    .antMatchers("/users/**").authenticated()
                    .antMatchers("/h2-console/**").permitAll()
                    .antMatchers("/api/**").authenticated()
                    .anyRequest().hasAnyRole("ADMIN", "USER")
                .and()
                    .headers().frameOptions().disable()
                .and()
                    .formLogin()
                    .loginProcessingUrl("/users/login")
                    .loginPage("/users/login")
                    .usernameParameter("email")
                    .passwordParameter("password")
                .and()
                    .oauth2Login()
                    .successHandler(successHandler)
                    .failureHandler((request,response,authentication)->response.sendRedirect("/error"))
                .and()
                    .logout()
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                    .logoutSuccessUrl("/users/login")
                .and()
                    .rememberMe()
                    .rememberMeCookieName("remember-me")
                    .key(REMEMBER_ME_KEY)
                    .userDetailsService(tokTalkUserDetailsService)
                    .tokenValiditySeconds(24 * 60 * 60) //1day
                    .tokenRepository(persistentTokenRepository())
                .and()
//                    .addFilterBefore(filter, BasicAuthenticationFilter.class)
                    .csrf().disable();
        }


//    @Bean
//    public UserAuthenticationSuccessHandler userAuthenticationSuccessHandler() {
//        return new UserAuthenticationSuccessHandler();
//    }


    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }
}