package com.jieyou.photography.module.photo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jieyou.photography.module.catalog.entity.Package;
import com.jieyou.photography.module.catalog.entity.Theme;
import com.jieyou.photography.module.catalog.mapper.PackageMapper;
import com.jieyou.photography.module.catalog.mapper.ThemeMapper;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 照片模块集成测试（API 约定 5.5），使用本机 MySQL 与 target/test-uploads 临时目录。
 * 覆盖：批量上传与轮次、白名单/大小/路径穿越校验、部分失败、水印预览、
 * 交付前禁止下载原图、交付后中文文件名下载、uploads 不暴露为静态资源。
 */
@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@AutoConfigureMockMvc
class PhotoModuleIntegrationTest {

    private static final Path UPLOAD_ROOT =
            Paths.get("target/test-uploads").toAbsolutePath().normalize();

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
    void uploadsPhotosAndIncrementsRoundAfterModification() throws Exception {
        Long userId = createUser("it_ph_owner");
        String token = userToken(userId, "it_ph_owner");
        Long orderId = createOrder(userId, "SHOOTING_DONE");

        MockMultipartFile first = imageFile("照片一.png", "png", 320, 240, Color.BLUE);
        MockMultipartFile second = imageFile("photo2.png", "png", 160, 120, Color.GREEN);
        JsonNode uploaded = upload(adminToken(), orderId, first, second);
        assertEquals(0, uploaded.path("code").asInt(), uploaded.toString());
        assertEquals(2, uploaded.path("data").path("success").size());
        assertEquals(0, uploaded.path("data").path("failed").size());

        assertEquals("UPLOADED", orderStatus(orderId));
        List<OrderLog> logs = orderLogs(orderId);
        assertEquals(1, logs.size());
        assertEquals("UPLOAD", logs.get(0).getAction());
        assertEquals("SHOOTING_DONE", logs.get(0).getFromStatus());
        assertEquals("UPLOADED", logs.get(0).getToStatus());
        assertEquals("ADMIN", logs.get(0).getOperatorRole());

        List<Photo> photos = photosOf(orderId);
        assertEquals(2, photos.size());
        for (Photo photo : photos) {
            assertEquals(1, photo.getUploadRound());
            Path original = resolve(photo.getFilePath());
            assertTrue(Files.isRegularFile(original), "原图应落盘: " + original);
            assertNotNull(photo.getWatermarkPath(), "水印应生成");
            Path watermark = resolve(photo.getWatermarkPath());
            assertTrue(Files.isRegularFile(watermark), "水印图应落盘: " + watermark);
            assertNotEquals(photo.getFilePath(), photo.getWatermarkPath());
            assertFalse(java.util.Arrays.equals(Files.readAllBytes(original),
                    Files.readAllBytes(watermark)), "水印图应与原图不同");
            assertNotNull(ImageIO.read(watermark.toFile()), "水印图应可解析");
        }

        // 用户列表：仅返回预览入口，不泄露磁盘路径
        MvcResult listResult = mockMvc.perform(get("/api/orders/{id}/photos", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].previewUrl")
                        .value("/api/orders/" + orderId + "/photos/" + photos.get(0).getId() + "/preview"))
                .andReturn();
        String rawList = listResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(rawList.contains("filePath"), "列表不得返回原图磁盘路径");
        assertFalse(rawList.contains("watermarkPath"), "列表不得返回水印磁盘路径");

        // 用户发起修改后重新上传：轮次 +1
        Order order = orderMapper.selectById(orderId);
        order.setStatus("MODIFYING");
        orderMapper.updateById(order);

        JsonNode reuploaded = upload(adminToken(), orderId,
                imageFile("修改轮.jpg", "jpg", 200, 150, Color.RED));
        assertEquals(1, reuploaded.path("data").path("success").size());
        assertEquals("UPLOADED", orderStatus(orderId));

        List<Photo> allPhotos = photosOf(orderId);
        assertEquals(3, allPhotos.size());
        assertEquals(2, allPhotos.get(2).getUploadRound());

        List<OrderLog> allLogs = orderLogs(orderId);
        assertEquals(2, allLogs.size());
        assertEquals("MODIFYING", allLogs.get(1).getFromStatus());
        assertEquals("UPLOADED", allLogs.get(1).getToStatus());
    }

    @Test
    void invalidFilesAreReportedWithoutBlockingValidOnes() throws Exception {
        Long userId = createUser("it_ph_partial");
        Long orderId = createOrder(userId, "SHOOTING_DONE");

        MockMultipartFile valid = imageFile("ok.png", "png", 80, 60, Color.CYAN);
        MockMultipartFile wrongExtension = new MockMultipartFile("files", "note.txt",
                "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile fakeImage = new MockMultipartFile("files", "fake.jpg",
                "image/jpeg", "not an image".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile traversal = imageFile("../../evil.png", "png", 40, 30, Color.MAGENTA);
        MockMultipartFile oversized = new MockMultipartFile("files", "big.png", "image/png",
                new byte[10 * 1024 * 1024 + 1]);

        JsonNode result = upload(adminToken(), orderId, valid, wrongExtension, fakeImage,
                traversal, oversized);
        assertEquals(1, result.path("data").path("success").size());
        assertEquals("ok.png", result.path("data").path("success").get(0).asText());
        assertEquals(4, result.path("data").path("failed").size());

        List<String> reasons = new ArrayList<>();
        result.path("data").path("failed").forEach(node -> reasons.add(node.path("reason").asText()));
        assertTrue(reasons.stream().anyMatch(reason -> reason.contains("JPG/JPEG/PNG")), reasons.toString());
        assertTrue(reasons.stream().anyMatch(reason -> reason.contains("内容与扩展名不符")), reasons.toString());
        assertTrue(reasons.stream().anyMatch(reason -> reason.contains("文件名不合法")), reasons.toString());
        assertTrue(reasons.stream().anyMatch(reason -> reason.contains("10MB")), reasons.toString());

        assertEquals("UPLOADED", orderStatus(orderId));
        assertEquals(1, photosOf(orderId).size());
        assertEquals(1, photosOf(orderId).get(0).getUploadRound());
    }

    @Test
    void rejectsIllegalStatusRoleAndEmptyUpload() throws Exception {
        Long userId = createUser("it_ph_status");
        String token = userToken(userId, "it_ph_status");
        Long pendingOrder = createOrder(userId, "PENDING");

        uploadExpect(adminToken(), pendingOrder, status().isConflict(), 40900,
                imageFile("a.png", "png", 40, 30, Color.GRAY));
        assertEquals("PENDING", orderStatus(pendingOrder));
        assertTrue(photosOf(pendingOrder).isEmpty());

        Long shootingOrder = createOrder(userId, "SHOOTING_DONE");
        uploadExpect(token, shootingOrder, status().isForbidden(), 40300,
                imageFile("b.png", "png", 40, 30, Color.GRAY));

        mockMvc.perform(multipart("/api/admin/orders/{id}/photos", shootingOrder)
                        .file(imageFile("c.png", "png", 40, 30, Color.GRAY)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        uploadExpect(adminToken(), shootingOrder, status().isBadRequest(), 40000);
        assertEquals("SHOOTING_DONE", orderStatus(shootingOrder));

        List<MockMultipartFile> tooMany = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tooMany.add(imageFile("p" + i + ".png", "png", 10, 10, Color.BLACK));
        }
        uploadExpect(adminToken(), shootingOrder, status().isBadRequest(), 40000,
                tooMany.toArray(new MockMultipartFile[0]));
        assertTrue(photosOf(shootingOrder).isEmpty());
    }

    @Test
    void previewIsWatermarkedBeforeDeliveryAndAccessIsRestricted() throws Exception {
        String ownerName = "it_ph_preview_owner";
        Long ownerId = createUser(ownerName);
        String ownerToken = userToken(ownerId, ownerName);
        String otherName = "it_ph_preview_other";
        Long otherId = createUser(otherName);
        String otherToken = userToken(otherId, otherName);
        Long orderId = createOrder(ownerId, "SHOOTING_DONE");

        upload(adminToken(), orderId, imageFile("水印测试.png", "png", 400, 300, Color.ORANGE));
        Photo photo = photosOf(orderId).get(0);
        byte[] originalBytes = Files.readAllBytes(resolve(photo.getFilePath()));
        byte[] watermarkBytes = Files.readAllBytes(resolve(photo.getWatermarkPath()));

        // 非本人：列表与预览均 403
        mockMvc.perform(get("/api/orders/{id}/photos", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/orders/{id}/photos/{photoId}/preview", orderId, photo.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // 交付前：用户预览必须是水印图
        MvcResult userPreview = mockMvc.perform(
                        get("/api/orders/{id}/photos/{photoId}/preview", orderId, photo.getId())
                                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andReturn();
        assertArrayEquals(watermarkBytes, userPreview.getResponse().getContentAsByteArray());
        assertFalse(java.util.Arrays.equals(originalBytes,
                userPreview.getResponse().getContentAsByteArray()), "交付前不得返回原图");

        // 管理员预览：原图
        MvcResult adminPreview = mockMvc.perform(
                        get("/api/orders/{id}/photos/{photoId}/preview", orderId, photo.getId())
                                .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertArrayEquals(originalBytes, adminPreview.getResponse().getContentAsByteArray());

        // 交付前下载原图：403
        mockMvc.perform(get("/api/orders/{id}/photos/{photoId}/download", orderId, photo.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // 照片不属于该订单 / 不存在：404
        mockMvc.perform(get("/api/orders/{id}/photos/{photoId}/preview", orderId, 999999999L)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));

        // uploads 目录不作为静态资源暴露（S-02）
        mockMvc.perform(get("/" + photo.getFilePath())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deliveredUserDownloadsOriginalWithEncodedChineseName() throws Exception {
        String ownerName = "it_ph_download";
        Long ownerId = createUser(ownerName);
        String ownerToken = userToken(ownerId, ownerName);
        String otherName = "it_ph_download_other";
        Long otherId = createUser(otherName);
        String otherToken = userToken(otherId, otherName);
        Long orderId = createOrder(ownerId, "SHOOTING_DONE");
        String chineseName = "精修照片一.png";

        upload(adminToken(), orderId, imageFile(chineseName, "png", 240, 180, Color.PINK));
        Photo photo = photosOf(orderId).get(0);
        byte[] originalBytes = Files.readAllBytes(resolve(photo.getFilePath()));

        Order order = orderMapper.selectById(orderId);
        order.setStatus("DELIVERED");
        order.setDeliverTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 交付后预览默认原图（P-06）
        MvcResult preview = mockMvc.perform(
                        get("/api/orders/{id}/photos/{photoId}/preview", orderId, photo.getId())
                                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        assertArrayEquals(originalBytes, preview.getResponse().getContentAsByteArray());

        String encodedName = URLEncoder.encode(chineseName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        MvcResult download = mockMvc.perform(
                        get("/api/orders/{id}/photos/{photoId}/download", orderId, photo.getId())
                                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''" + encodedName))
                .andReturn();
        assertArrayEquals(originalBytes, download.getResponse().getContentAsByteArray());

        // 管理员可下载原图
        mockMvc.perform(get("/api/orders/{id}/photos/{photoId}/download", orderId, photo.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        // 非本人即使已交付也不可下载
        mockMvc.perform(get("/api/orders/{id}/photos/{photoId}/download", orderId, photo.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    private JsonNode upload(String token, Long orderId, MockMultipartFile... files) throws Exception {
        MvcResult result = uploadRequest(token, orderId, files)
                .andExpect(status().isOk())
                .andReturn();
        return readBody(result);
    }

    private void uploadExpect(String token, Long orderId, ResultMatcher statusMatcher,
                              int code, MockMultipartFile... files) throws Exception {
        uploadRequest(token, orderId, files)
                .andExpect(statusMatcher)
                .andExpect(jsonPath("$.code").value(code));
    }

    private ResultActions uploadRequest(String token, Long orderId, MockMultipartFile... files)
            throws Exception {
        MockMultipartHttpServletRequestBuilder request =
                multipart("/api/admin/orders/{id}/photos", orderId);
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private MockMultipartFile imageFile(String name, String format, int width, int height, Color color)
            throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.WHITE);
            graphics.drawString("test", 5, Math.max(12, height / 2));
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output));
        String contentType = "png".equals(format) ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
        return new MockMultipartFile("files", name, contentType, output.toByteArray());
    }

    private Long createOrder(Long userId, String status) {
        Long themeId = createTheme();
        BigDecimal price = new BigDecimal("399.00");
        Package pkg = new Package();
        pkg.setThemeId(themeId);
        pkg.setName("集成测试-照片套餐");
        pkg.setPrice(price);
        pkg.setRetouchCount(10);
        pkg.setSort(0);
        pkg.setStatus(1);
        packageMapper.insert(pkg);
        packageIds.add(pkg.getId());

        Order order = new Order();
        order.setOrderNo("SOIT" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setUserId(userId);
        order.setThemeId(themeId);
        order.setPackageId(pkg.getId());
        order.setShootDate(LocalDate.now().plusDays(1));
        order.setShootSlot("MORNING");
        order.setStatus(status);
        order.setPrice(price);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        orderIds.add(order.getId());
        return order.getId();
    }

    private Long createTheme() {
        Theme theme = new Theme();
        theme.setName("集成测试-照片主题");
        theme.setSort(50);
        theme.setStatus(1);
        themeMapper.insert(theme);
        themeIds.add(theme.getId());
        return theme.getId();
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

    private String orderStatus(Long orderId) {
        return orderMapper.selectById(orderId).getStatus();
    }

    private List<Photo> photosOf(Long orderId) {
        return photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getOrderId, orderId)
                .orderByAsc(Photo::getUploadRound)
                .orderByAsc(Photo::getId));
    }

    private List<OrderLog> orderLogs(Long orderId) {
        return orderLogMapper.selectList(new LambdaQueryWrapper<OrderLog>()
                .eq(OrderLog::getOrderId, orderId)
                .orderByAsc(OrderLog::getId));
    }

    private String adminToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return readBody(result).path("data").path("token").asText();
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
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
