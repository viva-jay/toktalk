package com.chat.toktalk.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    public void 로그인성공테스트() throws Exception {
        this.mockMvc
                .perform(post("/users/login")
                        .param("email", "noriming2@gmail.com")
                        .param("password", "1234"))
                .andDo(print())
                .andExpect(cookie().exists("SESSION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    public void 로그인실패테스트() throws Exception {
        this.mockMvc
                .perform(post("/users/login")
                        .param("email", "noriming2@gmail.com")
                        .param("password", "1111"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/login?error"));
    }

    @Test
    public void 리멤버미로그인테스트() throws Exception {
        this.mockMvc
                .perform(post("/users/login")
                .param("email","noriming2@gmail.com")
                .param("password","1234")
                .param("remember-me","true"))
                .andDo(print())
        .andExpect(cookie().exists("remember-me"));
    }
}