package com.chat.toktalk.interceptor;

import com.chat.toktalk.security.LoginUserInfo;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        Authentication authentication = (Authentication) request.getPrincipal();

        if (authentication != null && authentication.getPrincipal() instanceof LoginUserInfo) {
            LoginUserInfo loginUserInfo = (LoginUserInfo) authentication.getPrincipal();
            attributes.put("userId", loginUserInfo.getId());
            attributes.put("nickname", loginUserInfo.getNickname());
            attributes.put("username", loginUserInfo.getEmail());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
