package com.chat.toktalk.config;

import com.chat.toktalk.amqp.MessageSender;
import com.chat.toktalk.domain.*;
import com.chat.toktalk.dto.SendType;
import com.chat.toktalk.dto.SocketMessage;
import com.chat.toktalk.service.*;
import com.chat.toktalk.websocket.SessionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@Component
public class CustomWebSocketHandler extends TextWebSocketHandler {

    private final SessionManager sessionManager;
    private final MessageService messageService;
    private final RedisService redisService;
    private final UserService userService;
    private final ChannelUserService channelUserService;
    private final ChannelService channelService;
    private final MessageSender messageSender;
    private final UploadFileService uploadFileService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomWebSocketHandler(SessionManager sessionManager, MessageService messageService, RedisService redisService, UserService userService, ChannelUserService channelUserService, ChannelService channelService, MessageSender messageSender, UploadFileService uploadFileService) {
        this.sessionManager = sessionManager;
        this.messageService = messageService;
        this.redisService = redisService;
        this.userService = userService;
        this.channelUserService = channelUserService;
        this.channelService = channelService;
        this.messageSender = messageSender;
        this.uploadFileService = uploadFileService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();

        logger.info("새로운 웹소켓 세션 id : " + session.getId());
        Long userId = userIdFrom(attributes);
        sessionManager.addWebSocketSession(userId, session);

        // Redis 웹소켓세션 등록
        redisService.addWebSocketSessionByUser(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>(){};
        HashMap<String, Object> map = objectMapper.readValue(message.getPayload(), typeRef);
        String type = stringOf(map, "type");
        if("pong".equals(type)) {
            return;
        }
        if ("typing".equals(type)) {
            alertTyping(session);
        }
        if ("switch".equals(type)) {
            switchChannel(session, map);
        }
        if ("chat".equals(type)) {
            handleChatMessage(session, map);
        }
        if ("invite_member".equals(type)) {
            inviteMember(map);
        }
        if ("invite_direct".equals(type)) {
            notifyInvitation(map);
        }
    }

    private void notifyInvitation(HashMap<String, Object> map) {
        Long channelId = channelIdFrom(map);
        Long userId = userIdFrom(map);
        Channel channel = channelService.getChannel(channelId);
        channel.setName(channel.getFirstUserName());
        messageSender.sendMessage(SocketMessage.channelJoinAlarm(SendType.CHANNEL_JOINED, userId, channel));
    }

    private void inviteMember(HashMap<String, Object> map) {
        Long invitedUserId = userIdFrom(map);
        Long channelId = channelIdFrom(map);
        Channel channel = channelService.getChannel(channelId);
        addNewChannelUser(stringOf(map, "nickname"), invitedUserId, channelId);
        messageSender.sendMessage(SocketMessage.channelJoinAlarm(SendType.CHANNEL_JOINED, invitedUserId, channel));
    }

    private void alertTyping(WebSocketSession session) {
        Long channelId = redisService.getActiveChannelInfo(session);
        String nickname = (String) session.getAttributes().get("nickname");
        Long userId = Long.parseLong(session.getAttributes().get("userId").toString());
        String typingAlarm = nickname + "님이 입력 중";
        messageSender.sendMessage(SocketMessage.typingAlarm(SendType.TYPING, userId, channelId, typingAlarm));
    }

    void switchChannel(WebSocketSession session, HashMap<String, Object> map) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        String username = stringOf(attributes, "username");
        String nickname = stringOf(attributes, "nickname");
        Long userId = userIdFrom(attributes);
        User user = userService.findUserByEmail(username);

        Long channelId = channelIdFrom(map);

        /*
         *   switch 하기 전 active channel 의 lastReadCnt 를 업데이트
         */
        Long activeChannelId = redisService.getActiveChannelInfo(session);
        if(activeChannelId != null){
            ChannelUser alreadyUser = channelUserService.getChannelUser(activeChannelId, user.getId());
            if (alreadyUser != null) {
//                alreadyUser.setLastReadCnt(redisService.getLastMessageIdByChannel(activeChannelId));
                alreadyUser.setLastReadCnt(messageService.getTotalMessageCntByChannel(activeChannelId));
                channelUserService.updateChannelUser(alreadyUser);
            }
        }

        /*
        *   active channel 바꾸기
        */
        if (channelId == 0) { //0번채널로 바꾸라는 요청은 그냥 지우라는 의미
            redisService.removeActiveChannelInfo(session);
            return;
        }

        String sessionId = session.getId();
        redisService.addActiveChannelInfo(sessionId, channelId);

        ChannelUser alreadyUser = channelUserService.getChannelUser(channelId, user.getId());
        List<Message> messages = null;

        // 이미 채널에 존재하는 유저
        if(alreadyUser != null){
            // 1. 마지막으로 읽은 메세지 id 업데이트
            // 2. firstReadId 기준으로 메세지 리스트 리턴
            Long firstReadId = alreadyUser.getFirstReadId();
            messages = messageService.getMessagesByChannelUser(channelId, firstReadId);
            if(messages != null && messages.size() > 0){
                alreadyUser.setLastReadCnt(messages.get(messages.size() - 1).getId());
                channelUserService.updateChannelUser(alreadyUser);
                SocketMessage socketMessage = new SocketMessage(SendType.MESSAGES);
                socketMessage.setChannelId(channelId);
                socketMessage.setMessages(messages);
                String jsonStr = new ObjectMapper().writeValueAsString(socketMessage);
                session.sendMessage(new TextMessage(jsonStr));
            }

            // 이 유저가 가진 웹소켓세션에도 이 채널 unread mark 지우라고 해야함
            messageSender.sendMessage(SocketMessage.markAsReadAlarm(SendType.CHANNEL_MARK, user.getId(), channelId));


        // 새롭게 채널에 합류한 유저
        }else{
            addNewChannelUser(nickname, userId, channelId);
        }
    }

    private void addNewChannelUser(String nickname, Long userId, Long channelId) {
        ChannelUser channelUser = new ChannelUser();
        channelUserService.addChannelUser(channelUser, userId, channelId);

        // 입장메세지 (채널 최초 생성 시 초대된 멤버는 입장메세지 X)
        if (Objects.nonNull(nickname)) {
            String notice = "[알림] \"" + nickname + "\" 님이 입장하셨습니다.";
            Message saved =
                    messageService.addMessage(Message.builder().type("system").channelId(channelId).text(notice).build());

            // firstReadId 업데이트
            channelUser.setFirstReadId(saved.getId());
            channelUserService.updateChannelUser(channelUser);

            messageSender.sendMessage(SocketMessage.systemAlarm(SendType.SYSTEM, channelId, notice));
        }
    }

    private void handleChatMessage(WebSocketSession session, HashMap<String, Object> map) {
        Long channelId = channelIdFrom(map);
        String message = stringOf(map, "text");
        String channelType = stringOf(map, "channelType");

        Map<String, Object> attributes = session.getAttributes();
        Long userId = userIdFrom(attributes);
        String nickname = stringOf(attributes, "nickname");

        messageService.addMessage(Message.builder()
                .userId(userId).nickname(nickname).channelId(channelId).channelType(ChannelType.valueOf(channelType)).text(message)
                .build());

        messageSender.sendMessage(SocketMessage.chatAlarm(SendType.CHAT, channelId, message, nickname));

        // TODO 첨부파일 있으면 보내기
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long userId = userIdFrom(attributes);
        sessionManager.removeWebSocketSession(userId, session);

        // 마지막으로 보고 있던 채널의 lastReadCnt 를 업데이트
        Long activeChannelId = redisService.getActiveChannelInfo(session);
        if(activeChannelId != null){
            ChannelUser alreadyUser = channelUserService.getChannelUser(activeChannelId, userId);
            alreadyUser.setLastReadCnt(messageService.getTotalMessageCntByChannel(activeChannelId));
            channelUserService.updateChannelUser(alreadyUser);
        }

        // Redis 웹소켓세션 삭제
        redisService.removeWebSocketSessionByUser(userId, session);
        redisService.removeActiveChannelInfo(session);
    }

    private String stringOf(Map<String, Object> map, String key) {
        Object obj = map.get(key);
        return obj instanceof String ? (String) obj : null;
    }

    private Long channelIdFrom(Map<String, Object> map) {
        Object obj = map.get("channelId");
        return obj instanceof String ? Long.parseLong((String) obj) : 0;
    }

    private Long userIdFrom(Map<String, Object> map) {
        Object obj = map.get("userId");
        return obj instanceof String ? Long.parseLong((String) obj) : 0;
    }
}