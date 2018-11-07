package com.chat.toktalk.config.handler;

import com.chat.toktalk.domain.User;
import com.chat.toktalk.security.LoginUserInfo;
import com.chat.toktalk.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
public class Oauth2AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final String ROLE_SOCIAL = "ROLE_USER";
    private static final String SOCIAL_EMAIL = "email";

    @Autowired
    UserService userService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, RuntimeException, ServletException {

        User user = userService.findUserByEmail(getSocialMail(authentication));

        if (user == null) {
            throw new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다.");
        }

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(getLoginUserInfo(user), null, getRoles()));

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private LoginUserInfo getLoginUserInfo(User user) {
        return new LoginUserInfo(user.getEmail(), user.getPassword(), getRoles(), user.getId(), user.getNickname());
    }

    private List<GrantedAuthority> getRoles() {
        return AuthorityUtils.createAuthorityList(ROLE_SOCIAL);
    }

    private String getSocialMail(Authentication authentication) {
        Map<String, Object> attributes = ((OAuth2AuthenticationToken) authentication).getPrincipal().getAttributes();
        return (String) attributes.get(SOCIAL_EMAIL);
    }
}