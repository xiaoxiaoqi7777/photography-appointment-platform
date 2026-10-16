package com.jieyou.photography.module.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jieyou.photography.module.user.entity.User;
import com.jieyou.photography.module.user.mapper.UserMapper;
import com.jieyou.photography.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户模块集成测试（API 约定 5.1），使用本机 MySQL 的 photography 库。
 * 覆盖：注册（成功/重复/非法参数/角色强制 USER）、个人信息维护（改昵称手机号/改密码/原密码错误/空提交）、
 * 以及阶段二遗留的禁用账号登录用例。
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerThenLoginThenMe() throws Exception {
        String username = "it_reg_ok";
        try {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"test123456\","
                                    + "\"nickname\":\"集成测试用户\",\"phone\":\"13800138000\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            String token = login(username, "test123456");

            User saved = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getUsername, username));
            assertNotNull(saved);
            assertTrue(saved.getPassword().startsWith("$2a$"), "库中密码必须为 BCrypt 密文");
            assertNotEquals("test123456", saved.getPassword());

            mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value(username))
                    .andExpect(jsonPath("$.data.nickname").value("集成测试用户"))
                    .andExpect(jsonPath("$.data.phone").value("13800138000"))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.data.status").value(1));
        } finally {
            deleteByUsername(username);
        }
    }

    @Test
    void registerDuplicateUsernameReturns40901() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("用户名已被占用"));
    }

    @Test
    void registerInvalidPhoneReturns40000() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it_reg_phone\",\"password\":\"test123456\",\"phone\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void registerShortPasswordReturns40000() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it_reg_pwd\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void registerForcesRoleUser() throws Exception {
        String username = "it_reg_role";
        try {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"test123456\","
                                    + "\"role\":\"ADMIN\",\"status\":0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            User saved = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getUsername, username));
            assertNotNull(saved);
            assertEquals("USER", saved.getRole(), "角色必须由服务端固定为 USER");
            assertEquals(1, saved.getStatus());
        } finally {
            deleteByUsername(username);
        }
    }

    @Test
    void updateNicknameAndPhone() throws Exception {
        Long id = createFixtureUser("it_upd_info", "test123456", 1);
        try {
            String token = jwtService.generateToken(id, "it_upd_info", "USER");
            MvcResult result = mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"新昵称\",\"phone\":\"13900139000\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                    .andExpect(jsonPath("$.data.phone").value("13900139000"))
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertFalse(body.path("data").has("password"), "响应不得包含 password 字段");

            User saved = userMapper.selectById(id);
            assertEquals("新昵称", saved.getNickname());
            assertEquals("13900139000", saved.getPhone());
        } finally {
            userMapper.deleteById(id);
        }
    }

    @Test
    void updateWithWrongOldPasswordReturns40000() throws Exception {
        Long id = createFixtureUser("it_upd_pwd_err", "test123456", 1);
        try {
            String token = jwtService.generateToken(id, "it_upd_pwd_err", "USER");
            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"wrong-pass\",\"newPassword\":\"new123456\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        } finally {
            userMapper.deleteById(id);
        }
    }

    @Test
    void updatePasswordThenLoginWithNewPassword() throws Exception {
        String username = "it_upd_pwd_ok";
        Long id = createFixtureUser(username, "test123456", 1);
        try {
            String token = jwtService.generateToken(id, username, "USER");
            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"test123456\",\"newPassword\":\"new123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"test123456\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(40100));

            assertNotNull(login(username, "new123456"));
        } finally {
            userMapper.deleteById(id);
        }
    }

    @Test
    void updateWithoutFieldsReturns40000() throws Exception {
        Long id = createFixtureUser("it_upd_empty", "test123456", 1);
        try {
            String token = jwtService.generateToken(id, "it_upd_empty", "USER");
            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        } finally {
            userMapper.deleteById(id);
        }
    }

    @Test
    void updateWithOnlyOnePasswordFieldReturns40000() throws Exception {
        Long id = createFixtureUser("it_upd_pwd_part", "test123456", 1);
        try {
            String token = jwtService.generateToken(id, "it_upd_pwd_part", "USER");
            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"test123456\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        } finally {
            userMapper.deleteById(id);
        }
    }

    @Test
    void disabledAccountLoginReturns403() throws Exception {
        Long id = createFixtureUser("it_disabled_user", "test123456", 0);
        try {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"it_disabled_user\",\"password\":\"test123456\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(40300))
                    .andExpect(jsonPath("$.message").value("账号已禁用"));
        } finally {
            userMapper.deleteById(id);
        }
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("data").path("token").asText();
        assertNotNull(token);
        return token;
    }

    private Long createFixtureUser(String username, String password, int status) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setStatus(status);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }

    private void deleteByUsername(String username) {
        userMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }
}
