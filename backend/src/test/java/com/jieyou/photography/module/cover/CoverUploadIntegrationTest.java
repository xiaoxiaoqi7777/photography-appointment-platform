package com.jieyou.photography.module.cover;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jieyou.photography.module.user.entity.User;
import com.jieyou.photography.module.user.mapper.UserMapper;
import com.jieyou.photography.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 封面上传集成测试：管理员上传、匿名读取、类型/大小/路径校验与公开范围限制。
 */
@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@AutoConfigureMockMvc
class CoverUploadIntegrationTest {

    private static final Path UPLOAD_ROOT =
            Paths.get("target/test-uploads").toAbsolutePath().normalize();
    private static final Path COVER_ROOT = UPLOAD_ROOT.resolve("covers");

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

    private final List<Path> uploadedFiles = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        uploadedFiles.forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {
                // 清理失败不影响测试结论
            }
        });
        userIds.forEach(userMapper::deleteById);
    }

    @Test
    void uploadsCoverAndServesItAnonymously() throws Exception {
        byte[] png = pngBytes(320, 200, Color.ORANGE);
        MockMultipartFile cover = new MockMultipartFile(
                "file", "主题封面.png", MediaType.IMAGE_PNG_VALUE, png);

        JsonNode body = upload(adminToken(), cover);
        assertEquals(0, body.path("code").asInt(), body.toString());
        String url = body.path("data").path("url").asText();
        assertTrue(url.matches("/api/covers/[0-9a-f]{32}\\.png"), url);
        assertFalse(body.toString().contains("主题封面"), "响应不得返回原始文件名");
        assertFalse(body.toString().contains("uploads"), "响应不得暴露磁盘路径");

        Path stored = coverPath(url);
        uploadedFiles.add(stored);
        assertTrue(Files.isRegularFile(stored), "封面应落盘: " + stored);

        // 匿名可读取，且字节与上传内容一致
        MvcResult fetched = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();
        assertArrayEquals(png, fetched.getResponse().getContentAsByteArray());

        // 同名文件重新上传必须得到不同的随机地址
        JsonNode second = upload(adminToken(), cover);
        String secondUrl = second.path("data").path("url").asText();
        uploadedFiles.add(coverPath(secondUrl));
        assertNotEquals(url, secondUrl);

        // 原 uploads/ 目录仍不暴露为静态资源（即使已登录也无对应静态资源）
        mockMvc.perform(get("/covers/" + stored.getFileName())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnauthorizedInvalidAndOversizedFiles() throws Exception {
        byte[] png = pngBytes(80, 60, Color.GREEN);
        long before = coverFileCount();

        uploadRequest(adminToken(), new MockMultipartFile(
                "file", "note.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        uploadRequest(adminToken(), new MockMultipartFile(
                "file", "fake.jpg", MediaType.IMAGE_JPEG_VALUE, "not an image".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        uploadRequest(adminToken(), new MockMultipartFile(
                "file", "wrong-content.jpg", MediaType.IMAGE_JPEG_VALUE, png))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        uploadRequest(adminToken(), new MockMultipartFile(
                "file", "../cover.png", MediaType.IMAGE_PNG_VALUE, png))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        uploadRequest(adminToken(), new MockMultipartFile(
                "file", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        uploadRequest(adminToken(), new MockMultipartFile(
                "file", "big.png", MediaType.IMAGE_PNG_VALUE, new byte[5 * 1024 * 1024 + 1]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        // 缺少 file 字段
        mockMvc.perform(multipart("/api/admin/covers")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        // 未登录 401；普通用户 403
        mockMvc.perform(multipart("/api/admin/covers").file(
                        new MockMultipartFile("file", "anonymous.png", MediaType.IMAGE_PNG_VALUE, png)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
        uploadRequest(userToken(), new MockMultipartFile(
                "file", "user.png", MediaType.IMAGE_PNG_VALUE, png))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        assertEquals(before, coverFileCount(), "校验失败的请求不应落盘");
    }

    @Test
    void onlyServerGeneratedCoverNamesAreReadable() throws Exception {
        mockMvc.perform(get("/api/covers/not-a-cover.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        mockMvc.perform(get("/api/covers/" + "a".repeat(32) + ".png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        mockMvc.perform(get("/api/covers/../application.yml")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().is4xxClientError());
    }

    private JsonNode upload(String token, MockMultipartFile file) throws Exception {
        MvcResult result = uploadRequest(token, file)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private ResultActions uploadRequest(String token, MockMultipartFile file) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/admin/covers");
        request.file(file);
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private byte[] pngBytes(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }

    private String adminToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("token").asText();
    }

    private String userToken() {
        String username = "it_cover_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("test123456"));
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        userIds.add(user.getId());
        return jwtService.generateToken(user.getId(), username, "USER");
    }

    private static Path coverPath(String url) {
        String filename = url.substring(url.lastIndexOf('/') + 1);
        return COVER_ROOT.resolve(filename).normalize();
    }

    private static long coverFileCount() throws Exception {
        if (!Files.isDirectory(COVER_ROOT)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(COVER_ROOT)) {
            return files.filter(Files::isRegularFile).count();
        }
    }
}
