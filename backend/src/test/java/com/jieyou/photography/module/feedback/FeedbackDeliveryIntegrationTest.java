package com.jieyou.photography.module.feedback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jieyou.photography.module.catalog.entity.Package;
import com.jieyou.photography.module.catalog.entity.Theme;
import com.jieyou.photography.module.catalog.mapper.PackageMapper;
import com.jieyou.photography.module.catalog.mapper.ThemeMapper;
import com.jieyou.photography.module.feedback.entity.Feedback;
import com.jieyou.photography.module.feedback.mapper.FeedbackMapper;
import com.jieyou.photography.module.order.entity.Order;
import com.jieyou.photography.module.order.entity.OrderLog;
import com.jieyou.photography.module.order.mapper.OrderLogMapper;
import com.jieyou.photography.module.order.mapper.OrderMapper;
import com.jieyou.photography.module.photo.entity.Photo;
import com.jieyou.photography.module.photo.mapper.PhotoMapper;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 反馈与交付模块集成测试（API 约定 5.6/5.7），使用本机 MySQL 与临时上传目录。
 * 覆盖：完整业务闭环、反馈轮次与照片轮次对应、权限与状态校验、交付幂等、交付后禁止反馈。
 */
@SpringBootTest(properties = "app.upload-dir=target/test-feedback-uploads")
@AutoConfigureMockMvc
class FeedbackDeliveryIntegrationTest {

    private static final Path UPLOAD_ROOT =
            Paths.get("target/test-feedback-uploads").toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private PhotoMapper photoMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private ThemeMapper themeMapper;

    @Autowired
    private PackageMapper packageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<Long> orderIds = new ArrayList<>();
    private final List<Long> packageIds = new ArrayList<>();
    private final List<Long> themeIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long orderId : orderIds) {
            feedbackMapper.delete(new LambdaQueryWrapper<Feedback>().eq(Feedback::getOrderId, orderId));
            photoMapper.delete(new LambdaQueryWrapper<Photo>().eq(Photo::getOrderId, orderId));
            orderLogMapper.delete(new LambdaQueryWrapper<OrderLog>().eq(OrderLog::getOrderId, orderId));
            orderMapper.deleteById(orderId);
            deleteDirectory(UPLOAD_ROOT.resolve(String.valueOf(orderId)));
        }
        packageIds.forEach(packageMapper::deleteById);
        themeIds.forEach(themeMapper::deleteById);
        userIds.forEach(userMapper::deleteById);
    }

    @Test
    void fullClosedLoopFromOrderToOriginalDownload() throws Exception {
        String username = "it_fb_loop";
        Long userId = createUser(username);
        String token = userToken(userId, username);
        String adminToken = adminToken();
        Long themeId = createTheme();
        Long packageId = createPackage(themeId);

        Long orderId = createOrderViaHttp(token, themeId, packageId);
        mockMvc.perform(post("/api/admin/orders/{id}/confirm", orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/orders/{id}/shoot-done", orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        upload(adminToken, orderId, imageFile("第一轮.png", 320, 240, Color.BLUE));
        JsonNode firstList = performJson(get("/api/orders/{id}/photos", orderId), token);
        assertEquals(1, firstList.path("data").size());
        Long firstPhotoId = firstList.path("data").get(0).path("id").asLong();
        mockMvc.perform(get("/api/orders/{id}/photos/{photoId}/preview", orderId, firstPhotoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));

        JsonNode firstFeedback = performJson(post("/api/orders/{id}/feedback", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"请把背景调亮一些\"}"), null);
        assertEquals(1, firstFeedback.path("data").path("round").asInt());
        assertEquals(1, firstFeedback.path("data").path("photoRound").asInt());
        assertEquals("请把背景调亮一些", firstFeedback.path("data").path("content").asText());
        assertNotNull(firstFeedback.path("data").path("createTime").asText(null));
        assertEquals("MODIFYING", orderStatus(orderId));

        upload(adminToken, orderId, imageFile("第二轮.png", 300, 200, Color.GREEN));
        List<Photo> photos = photosOf(orderId);
        assertEquals(2, photos.size());
        assertEquals(1, photos.get(0).getUploadRound());
        assertEquals(2, photos.get(1).getUploadRound());
        Photo deliveredPhoto = photos.get(1);
        byte[] originalBytes = Files.readAllBytes(resolve(deliveredPhoto.getFilePath()));

        // 第二轮交付前预览仍为水印图
        mockMvc.perform(get("/api/orders/{id}/photos/{photoId}/preview", orderId, deliveredPhoto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode delivered = performJson(post("/api/orders/{id}/deliver", orderId)
                .header("Authorization", "Bearer " + token), null);
        assertEquals("DELIVERED", delivered.path("data").path("status").asText());
        assertNotNull(delivered.path("data").path("deliverTime").asText(null));

        String encodedName = URLEncoder.encode(deliveredPhoto.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        MvcResult download = mockMvc.perform(
                        get("/api/orders/{id}/photos/{photoId}/download", orderId, deliveredPhoto.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''" + encodedName))
                .andReturn();
        assertArrayEquals(originalBytes, download.getResponse().getContentAsByteArray());

        // 交付后不可再反馈
        mockMvc.perform(post("/api/orders/{id}/feedback", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"再改一版\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        JsonNode detail = performJson(get("/api/orders/{id}", orderId), token);
        List<String> actions = new ArrayList<>();
        detail.path("data").path("timeline").forEach(node -> actions.add(node.path("action").asText()));
        assertEquals(List.of("CREATE", "CONFIRM", "SHOOT_DONE", "UPLOAD", "MODIFY", "UPLOAD", "DELIVER"),
                actions);
        assertEquals(1, detail.path("data").path("feedbacks").size());

        JsonNode feedbacks = performJson(get("/api/orders/{id}/feedbacks", orderId), token);
        assertEquals(1, feedbacks.path("data").size());
        assertEquals(1, feedbacks.path("data").get(0).path("photoRound").asInt());

        // 管理员可查看反馈历史
        performJson(get("/api/orders/{id}/feedbacks", orderId), adminToken);
    }

    @Test
    void feedbackRoundsTrackPhotoRounds() throws Exception {
        String username = "it_fb_rounds";
        Long userId = createUser(username);
        String token = userToken(userId, username);
        Long orderId = createOrderDirect(userId, "UPLOADED");
        insertPhoto(orderId, 1);

        JsonNode first = submitFeedback(token, orderId, "第一轮意见");
        assertEquals(1, first.path("round").asInt());
        assertEquals(1, first.path("photoRound").asInt());

        insertPhoto(orderId, 2);
        setOrderStatus(orderId, "UPLOADED");
        JsonNode second = submitFeedback(token, orderId, "第二轮意见");
        assertEquals(2, second.path("round").asInt());
        assertEquals(2, second.path("photoRound").asInt());

        insertPhoto(orderId, 3);
        setOrderStatus(orderId, "UPLOADED");
        JsonNode third = submitFeedback(token, orderId, "第三轮意见");
        assertEquals(3, third.path("round").asInt());
        assertEquals(3, third.path("photoRound").asInt());

        JsonNode list = performJson(get("/api/orders/{id}/feedbacks", orderId), token);
        assertEquals(3, list.path("data").size());
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, list.path("data").get(i).path("round").asInt());
            assertEquals(i + 1, list.path("data").get(i).path("photoRound").asInt());
        }
        assertEquals(3, feedbackLogs(orderId));
    }

    @Test
    void feedbackValidatesContentRoleOwnershipAndStatus() throws Exception {
        String ownerName = "it_fb_owner";
        Long ownerId = createUser(ownerName);
        String ownerToken = userToken(ownerId, ownerName);
        String otherName = "it_fb_other";
        Long otherId = createUser(otherName);
        String otherToken = userToken(otherId, otherName);
        Long orderId = createOrderDirect(ownerId, "UPLOADED");
        insertPhoto(orderId, 1);

        // 空内容 / 超长内容 / 缺少字段
        submitExpect(ownerToken, orderId, "{\"content\":\"   \"}", 400, 40000);
        submitExpect(ownerToken, orderId, jsonContent("意".repeat(501)), 400, 40000);
        submitExpect(ownerToken, orderId, "{}", 400, 40000);

        // 非本人 / 管理员
        submitExpect(otherToken, orderId, jsonContent("越权"), 403, 40300);
        mockMvc.perform(get("/api/orders/{id}/feedbacks", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        submitExpect(adminToken(), orderId, jsonContent("管理员提交"), 403, 40300);

        // 未登录
        mockMvc.perform(post("/api/orders/{id}/feedback", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent("未登录")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        // 订单不存在
        submitExpect(ownerToken, 999999999L, jsonContent("订单不存在"), 404, 40400);

        // 非 UPLOADED 状态
        Long pendingOrder = createOrderDirect(ownerId, "PENDING");
        submitExpect(ownerToken, pendingOrder, jsonContent("状态不对"), 409, 40900);

        // 提交成功后状态变为 MODIFYING，未重新上传前不可再次提交
        assertEquals(1, submitFeedback(ownerToken, orderId, "有效意见").path("round").asInt());
        assertEquals("MODIFYING", orderStatus(orderId));
        submitExpect(ownerToken, orderId, jsonContent("重复提交"), 409, 40900);

        // 管理员可查看反馈历史
        JsonNode adminView = performJson(get("/api/orders/{id}/feedbacks", orderId), adminToken());
        assertEquals(1, adminView.path("data").size());
    }

    @Test
    void deliverRequiresOwnerAndUploadedStateAndIsIdempotent() throws Exception {
        String ownerName = "it_fb_deliver";
        Long ownerId = createUser(ownerName);
        String ownerToken = userToken(ownerId, ownerName);
        String otherName = "it_fb_deliver_other";
        Long otherId = createUser(otherName);
        String otherToken = userToken(otherId, otherName);
        Long orderId = createOrderDirect(ownerId, "UPLOADED");
        insertPhoto(orderId, 1);

        // 无权限 / 角色不符
        mockMvc.perform(post("/api/orders/{id}/deliver", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(post("/api/orders/{id}/deliver", orderId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(post("/api/orders/{id}/deliver", orderId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        // 非 UPLOADED 状态不可交付
        Long shootingOrder = createOrderDirect(ownerId, "SHOOTING_DONE");
        mockMvc.perform(post("/api/orders/{id}/deliver", shootingOrder)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        // 订单不存在
        mockMvc.perform(post("/api/orders/{id}/deliver", 999999999L)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));

        // 正常交付
        JsonNode delivered = performJson(post("/api/orders/{id}/deliver", orderId)
                .header("Authorization", "Bearer " + ownerToken), null);
        assertEquals("DELIVERED", delivered.path("data").path("status").asText());
        assertNotNull(orderMapper.selectById(orderId).getDeliverTime());

        List<OrderLog> logs = logsOf(orderId);
        assertEquals(1, logs.size());
        assertEquals("DELIVER", logs.get(0).getAction());
        assertEquals("UPLOADED", logs.get(0).getFromStatus());
        assertEquals("DELIVERED", logs.get(0).getToStatus());
        assertEquals("USER", logs.get(0).getOperatorRole());

        // 重复交付 / 交付后反馈
        mockMvc.perform(post("/api/orders/{id}/deliver", orderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
        submitExpect(ownerToken, orderId, jsonContent("交付后反馈"), 409, 40900);
        assertEquals(1, logsOf(orderId).size());
    }

    private JsonNode submitFeedback(String token, Long orderId, String content) throws Exception {
        JsonNode response = performJson(post("/api/orders/{id}/feedback", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent(content)), null);
        assertEquals(0, response.path("code").asInt(), response.toString());
        return response.path("data");
    }

    private void submitExpect(String token, Long orderId, String body, int httpStatus, int code)
            throws Exception {
        mockMvc.perform(post("/api/orders/{id}/feedback", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code));
    }

    private JsonNode upload(String adminToken, Long orderId, MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder request =
                multipart("/api/admin/orders/{id}/photos", orderId);
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        MvcResult result = mockMvc.perform(request.header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = readBody(result);
        assertEquals(0, body.path("code").asInt(), body.toString());
        assertEquals(files.length, body.path("data").path("success").size());
        return body;
    }

    private MockMultipartFile imageFile(String name, int width, int height, Color color) throws Exception {
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
        return new MockMultipartFile("files", name, MediaType.IMAGE_PNG_VALUE, output.toByteArray());
    }

    private Long createOrderViaHttp(String token, Long themeId, Long packageId) throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("themeId", themeId);
        body.put("packageId", packageId);
        body.put("shootDate", LocalDate.now().plusDays(3));
        body.put("shootSlot", "FULL_DAY");
        body.put("remark", "闭环测试");
        JsonNode response = performJson(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)), null);
        assertEquals(0, response.path("code").asInt(), response.toString());
        Long orderId = response.path("data").path("id").asLong();
        orderIds.add(orderId);
        return orderId;
    }

    private Long createOrderDirect(Long userId, String status) {
        Long themeId = createTheme();
        Long packageId = createPackage(themeId);
        Order order = new Order();
        order.setOrderNo("SOFB" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setUserId(userId);
        order.setThemeId(themeId);
        order.setPackageId(packageId);
        order.setShootDate(LocalDate.now().plusDays(1));
        order.setShootSlot("MORNING");
        order.setStatus(status);
        order.setPrice(new BigDecimal("399.00"));
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        orderIds.add(order.getId());
        return order.getId();
    }

    private Photo insertPhoto(Long orderId, int round) {
        Photo photo = new Photo();
        photo.setOrderId(orderId);
        photo.setOriginalName("第" + round + "轮.png");
        photo.setFilePath(orderId + "/fake-" + UUID.randomUUID() + ".png");
        photo.setFileSize(10L);
        photo.setUploadRound(round);
        photo.setCreateTime(LocalDateTime.now());
        photoMapper.insert(photo);
        return photo;
    }

    private Long createTheme() {
        Theme theme = new Theme();
        theme.setName("集成测试-反馈主题");
        theme.setSort(50);
        theme.setStatus(1);
        themeMapper.insert(theme);
        themeIds.add(theme.getId());
        return theme.getId();
    }

    private Long createPackage(Long themeId) {
        Package pkg = new Package();
        pkg.setThemeId(themeId);
        pkg.setName("集成测试-反馈套餐");
        pkg.setPrice(new BigDecimal("499.00"));
        pkg.setRetouchCount(10);
        pkg.setSort(0);
        pkg.setStatus(1);
        packageMapper.insert(pkg);
        packageIds.add(pkg.getId());
        return pkg.getId();
    }

    private Long createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("test123456"));
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        userIds.add(user.getId());
        return user.getId();
    }

    private String userToken(Long userId, String username) {
        return jwtService.generateToken(userId, username, "USER");
    }

    private String adminToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return readBody(result).path("data").path("token").asText();
    }

    private void setOrderStatus(Long orderId, String status) {
        Order order = orderMapper.selectById(orderId);
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private String orderStatus(Long orderId) {
        return orderMapper.selectById(orderId).getStatus();
    }

    private List<Photo> photosOf(Long orderId) {
        return photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getOrderId, orderId)
                .orderByAsc(Photo::getUploadRound)
                .orderByAsc(Photo::getId));
    }

    private List<OrderLog> logsOf(Long orderId) {
        return orderLogMapper.selectList(new LambdaQueryWrapper<OrderLog>()
                .eq(OrderLog::getOrderId, orderId)
                .orderByAsc(OrderLog::getId));
    }

    private long feedbackLogs(Long orderId) {
        return logsOf(orderId).stream().filter(log -> "MODIFY".equals(log.getAction())).count();
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 String token) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return readBody(result);
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String jsonContent(String content) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("content", content));
    }

    private static Path resolve(String storedPath) {
        return UPLOAD_ROOT.resolve(storedPath).normalize();
    }

    private static void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // 清理失败不影响测试结论
                }
            });
        } catch (Exception ignored) {
            // 清理失败不影响测试结论
        }
    }
}
