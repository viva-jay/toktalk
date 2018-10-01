package com.chat.toktalk.dto;

import com.chat.toktalk.domain.Channel;
import com.chat.toktalk.domain.Message;
import com.chat.toktalk.domain.UploadFile;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
@ToString
public class SocketMessage implements Serializable {
    private final SendType type;
    private Long channelId;
    private Long userId;
    private String nickname;
    private String text;
    private Boolean notification;
    private List<Message> messages;
    private List<UnreadMessageInfo> unreadMessages;
    private UploadFile uploadFile;
    private Channel channel;
    private final LocalDateTime date;
    private final String regdate;

    public SocketMessage(SendType type) {
        this.type = type;
        date = LocalDateTime.now();
        regdate = date.format(DateTimeFormatter.ofPattern("a h:mm"));
    }

    public static SocketMessage channelJoinAlarm(SendType type, Long userId, Channel channel) {
        SocketMessage sm = new SocketMessage(type);
        sm.setUserId(userId);
        sm.setChannel(channel);
        return sm;
    }

    public static SocketMessage typingAlarm(SendType type, Long userId, Long channelId, String text) {
        SocketMessage sm = new SocketMessage(type);
        sm.setUserId(userId);
        sm.setChannelId(channelId);
        sm.setText(text);
        return sm;
    }

    public static SocketMessage markAsReadAlarm(SendType type, Long userId, Long channelId) {
        SocketMessage sm = new SocketMessage(type);
        sm.setUserId(userId);
        sm.setChannelId(channelId);
        return sm;
    }


    public static SocketMessage systemAlarm(SendType type, Long channelId, String text) {
        SocketMessage sm = new SocketMessage(type);
        sm.setChannelId(channelId);
        sm.setText(text);
        return sm;
    }

    public static SocketMessage chatAlarm(SendType type, Long channelId, String text, String nickname) {
        SocketMessage sm = new SocketMessage(type);
        sm.setChannelId(channelId);
        sm.setText(text);
        sm.setNickname(nickname);
        return sm;
    }
}