package com.chat.toktalk.domain;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Getter
@Setter
@Table(name = "user")
public class User implements Serializable {
    public User() {
        this.regdate = LocalDateTime.now();
    }

    @Builder
    public User(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String password;
    private String nickname;
    // private UploadFile uploadFile; TODO 1:1 인데 어떻게 하지
    private LocalDateTime regdate;
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChannelUser> channelUsers = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserRole> roles = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserOauthInfo> oauthInfos = new ArrayList<>();

    public void addChanneUser(ChannelUser channelUser){
        channelUsers.add(channelUser);
        if(channelUser.getUser() != this){
            channelUser.setUser(this);
        }
    }

    public void addUserRole(UserRole role){
        this.roles.add(role);
        if(role.getUser()!=this){
            role.setUser(this);
        }
    }
    public void addUserOauthInfo(UserOauthInfo oauthInfo){
        oauthInfos.add(oauthInfo);
        if(oauthInfo.getUser() != this){
            oauthInfo.setUser(this);
        }
    }
}
