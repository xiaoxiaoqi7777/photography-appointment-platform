package com.jieyou.photography.module.order;

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
import com.jieyou.photography.module.user.entity.User;
import com.jieyou.photography.module.user.mapper.UserMapper;
import com.jieyou.photography.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单模块集成测试（API 约定 5.4），使用本机 MySQL 的 photography 库。
 * 覆盖：下单快照与 CREATE 日志、下单校验、本人权限、取消、管理端确认/拒绝/拍摄完成、
 * 列表筛选与关键字搜索、并发重复确认仅一次成功。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

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
            orderLogMapper.delete(new LambdaQueryWrapper<OrderLog>().eq(OrderLog::getOrderId, orderId));
            feedbackMapper.delete(new LambdaQueryWrapper<Feedback>().eq(Feedback::getOrderId, orderId));
            orderMapper.deleteById(orderId);
        }
        packageIds.forEach(packageMapper::deleteById);
        themeIds.forEach(themeMapper::deleteById);
        userIds.forEach(userMapper::deleteById);
    }

    @Test
    void createOrderSnapshotsPriceAndWritesCreateLog() throws Exception {
        String username = "it_ord_create";
        Long userId = createUser(username);
        String token = jwtService.generateToken(userId, username, "USER");
        Long themeId = createTheme("集成测试-订单主题A", 1);
        Long packageId = createPackage(themeId, new BigDecimal("399.00"), 1);

        JsonNode created = createOrder(token, themeId, packageId, LocalDate.now().plusDays(3), "希望拍夜景");
        Long orderId = created.path("id").asLong();
        assertEquals("PENDING", created.path("status").asText());
        assertTrue(created.path("orderNo").asText().startsWith("SO"));
        assertTrue(created.path("orderNo").asText().length() <= 32);

        Order order = orderMapper.selectById(orderId);
        assertNotNull(order);
        assertEquals(userId, order.getUserId());
        assertEquals(themeId, order.getThemeId());
        assertEquals(packageId, order.getPackageId());
        assertEquals(LocalDate.now().plusDays(3), order.getShootDate());
        assertEquals("MORNING", order.getShootSlot());
        assertEquals("希望拍夜景", order.getRemark());
        assertEquals("PENDING", order.getStatus());
        assertEquals(0, new BigDecimal("399.00").compareTo(order.getPrice()));

        List<OrderLog> logs = logsOf(orderId);
        assertEquals(1, logs.size(), "创建订单必须写入一条 CREATE 日志");
        assertEquals("CREATE", logs.get(0).getAction());
        assertNull(logs.get(0).getFromStatus());
        assertEquals("PENDING", logs.get(0).getToStatus());
        assertEquals("USER", logs.get(0).getOperatorRole());
        assertEquals(userId, logs.get(0).getOperatorId());

        // 价格快照：套餐后续改价不影响已创建订单
        Package pkg = packageMapper.selectById(packageId);
        pkg.setPrice(new BigDecimal("999.00"));
        packageMapper.updateById(pkg);

        JsonNode detail = performJson(get("/api/orders/{id}", orderId), token);
        assertEquals(0, new BigDecimal("399.00")
                .compareTo(detail.path("data").path("order").path("price").decimalValue()));
        assertEquals("上午 09:00-12:00", detail.path("data").path("order").path("shootSlotText").asText());
        assertEquals("集成测试-订单主题A", detail.path("data").path("order").path("themeName").asText());
        assertEquals(1, detail.path("data").path("timeline").size());
        assertEquals(0, detail.path("data").path("feedbacks").size());

        JsonNode myOrders = performJson(get("/api/orders")
                .param("status", "PENDING").param("pageSize", "100"), token);
        assertTrue(listContains(myOrders, orderId), "我的订单应包含刚创建的订单");
    }

    @Test
    void createOrderValidatesRequestAndRole() throws Exception {
        String username = "it_ord_valid";
        Long userId = createUser(username);
        String token = jwtService.generateToken(userId, username, "USER");
        Long themeId = createTheme("集成测试-订单主题B", 1);
        Long otherThemeId = createTheme("集成测试-订单主题B2", 1);
        Long packageId = createPackage(themeId, new BigDecimal("199.00"), 1);
        Long offlinePackageId = createPackage(themeId, new BigDecimal("299.00"), 0);

        postOrder(token, themeId, packageId, LocalDate.now().minusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        postOrder(token, otherThemeId, packageId, LocalDate.now())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        postOrder(token, themeId, offlinePackageId, LocalDate.now())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        postOrder(token, themeId, 999999999L, LocalDate.now())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        // 管理员不可提交预约
        postOrder(adminToken(), themeId, packageId, LocalDate.now())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // 未登录
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(orderBody(themeId, packageId, LocalDate.now()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void cancelOnlyByOwnerAndOnlyWhenPending() throws Exception {
        String username = "it_ord_cancel";
        Long userId = createUser(username);
        String token = jwtService.generateToken(userId, username, "USER");
        String otherUsername = "it_ord_other";
        Long otherId = createUser(otherUsername);
        String otherToken = jwtService.generateToken(otherId, otherUsername, "USER");
        Long themeId = createTheme("集成测试-订单主题C", 1);
        Long packageId = createPackage(themeId, new BigDecimal("499.00"), 1);
        Long orderId = createOrder(token, themeId, packageId, LocalDate.now().plusDays(5), null)
                .path("id").asLong();

        // 非本人：查看/取消均 403
        mockMvc.perform(get("/api/orders/{id}", orderId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        List<OrderLog> logs = logsOf(orderId);
        assertEquals(2, logs.size());
        OrderLog cancelLog = logs.get(1);
        assertEquals("CANCEL", cancelLog.getAction());
        assertEquals("PENDING", cancelLog.getFromStatus());
        assertEquals("CANCELLED", cancelLog.getToStatus());
        assertEquals("USER", cancelLog.getOperatorRole());

        // 重复取消 / 管理员确认已取消订单 → 40900
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
        mockMvc.perform(post("/api/admin/orders/{id}/confirm", orderId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void adminConfirmRejectAndShootDone() throws Exception {
        String username = "it_ord_admin";
        Long userId = createUser(username);
        String token = jwtService.generateToken(userId, username, "USER");
        Long themeId = createTheme("集成测试-订单主题D", 1);
        Long packageId = createPackage(themeId, new BigDecimal("599.00"), 1);
        Long orderA = createOrder(token, themeId, packageId, LocalDate.now().plusDays(2), null)
                .path("id").asLong();
        Long orderB = createOrder(token, themeId, packageId, LocalDate.now().plusDays(4), null)
                .path("id").asLong();
        String adminToken = adminToken();

        mockMvc.perform(post("/api/admin/orders/{id}/confirm", orderA)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        mockMvc.perform(post("/api/admin/orders/{id}/confirm", orderA)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        String reason = "档期冲突，请改约";
        mockMvc.perform(post("/api/admin/orders/{id}/reject", orderB)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"" + reason + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReason").value(reason));
        mockMvc.perform(post("/api/admin/orders/{id}/reject", orderB)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"再试一次\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        mockMvc.perform(post("/api/admin/orders/{id}/shoot-done", orderA)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHOOTING_DONE"));
        mockMvc.perform(post("/api/admin/orders/{id}/shoot-done", orderA)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        // 拒绝原因为空 → 40000（参数校验）
        Long orderC = createOrder(token, themeId, packageId, LocalDate.now().plusDays(6), null)
                .path("id").asLong();
        mockMvc.perform(post("/api/admin/orders/{id}/reject", orderC)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        List<OrderLog> logsA = logsOf(orderA);
        assertEquals(3, logsA.size(), "CREATE / CONFIRM / SHOOT_DONE 各一条");
        assertEquals("CONFIRM", logsA.get(1).getAction());
        assertEquals("PENDING", logsA.get(1).getFromStatus());
        assertEquals("CONFIRMED", logsA.get(1).getToStatus());
        assertEquals("SHOOT_DONE", logsA.get(2).getAction());
        assertEquals("CONFIRMED", logsA.get(2).getFromStatus());
        assertEquals("SHOOTING_DONE", logsA.get(2).getToStatus());
        assertEquals("ADMIN", logsA.get(2).getOperatorRole());

        List<OrderLog> logsB = logsOf(orderB);
        assertEquals(2, logsB.size());
        assertEquals("REJECT", logsB.get(1).getAction());
        assertEquals(reason, logsB.get(1).getRemark());

        JsonNode adminDetail = performJson(get("/api/admin/orders/{id}", orderA), adminToken);
        assertEquals("PENDING", adminDetail.path("data").path("timeline").get(1).path("fromStatus").asText());
        assertEquals("SHOOT_DONE", adminDetail.path("data").path("timeline").get(2).path("action").asText());
    }

    @Test
    void adminListSupportsStatusFilterAndKeywordSearch() throws Exception {
        String username = "it_ord_kw_user";
        Long userId = createUser(username);
        String token = jwtService.generateToken(userId, username, "USER");
        String themeName = "集成测试-关键字主题";
        Long themeId = createTheme(themeName, 1);
        Long packageId = createPackage(themeId, new BigDecimal("299.00"), 1);
        Long orderId = createOrder(token, themeId, packageId, LocalDate.now().plusDays(9), null)
                .path("id").asLong();
        String adminToken = adminToken();

        JsonNode byUsername = performJson(get("/api/admin/orders")
                .param("status", "PENDING").param("keyword", username).param("pageSize", "100"), adminToken);
        assertTrue(listContains(byUsername, orderId), "关键字应匹配用户名");

        JsonNode byTheme = performJson(get("/api/admin/orders")
                .param("keyword", themeName).param("pageSize", "100"), adminToken);
        assertTrue(listContains(byTheme, orderId), "关键字应匹配主题名");

        JsonNode noMatch = performJson(get("/api/admin/orders")
                .param("keyword", "不存在的关键字xyz").param("pageSize", "100"), adminToken);
        assertTrue(!listContains(noMatch, orderId), "无匹配关键字不应返回该订单");

        JsonNode confirmedOnly = performJson(get("/api/admin/orders")
                .param("status", "CONFIRMED").param("pageSize", "100"), adminToken);
        assertTrue(!listContains(confirmedOnly, orderId), "状态筛选不应返回 PENDING 订单");

        // 用户不能访问管理端
        mockMvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(get("/api/admin/orders/{id}", 999999999L).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void concurrentConfirmOnlyOneSucceeds() throws Exception {
        String username = "it_ord_concurrent";
        Long userId = createUser(username);
        String token = jwtService.generateToken(userId, username, "USER");
        Long themeId = createTheme("集成测试-订单主题E", 1);
        Long packageId = createPackage(themeId, new BigDecimal("799.00"), 1);
        Long orderId = createOrder(token, themeId, packageId, LocalDate.now().plusDays(8), null)
                .path("id").asLong();
        String adminToken = adminToken();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Integer> call = () -> {
                start.await(5, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/admin/orders/{id}/confirm", orderId)
                                .header("Authorization", "Bearer " + adminToken))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = pool.submit(call);
            Future<Integer> second = pool.submit(call);
            start.countDown();
            int statusA = first.get(15, TimeUnit.SECONDS);
            int statusB = second.get(15, TimeUnit.SECONDS);

            int success = (statusA == 200 ? 1 : 0) + (statusB == 200 ? 1 : 0);
            int conflict = (statusA == 409 ? 1 : 0) + (statusB == 409 ? 1 : 0);
            assertEquals(1, success, "并发确认只允许一次成功，实际状态码: " + statusA + "/" + statusB);
            assertEquals(1, conflict, "另一次应返回 409，实际状态码: " + statusA + "/" + statusB);
        } finally {
            pool.shutdownNow();
        }

        assertEquals("CONFIRMED", orderMapper.selectById(orderId).getStatus());
        long confirmLogs = logsOf(orderId).stream().filter(log -> "CONFIRM".equals(log.getAction())).count();
        assertEquals(1, confirmLogs, "并发确认只应写入一条 CONFIRM 日志");
    }

    @Test
    void invalidStatusFilterReturns40000() throws Exception {
        String username = "it_ord_filter";
        Long userId = createUser(username);
        String token = jwtService.generateToken(userId, username, "USER");
        mockMvc.perform(get("/api/orders").param("status", "NOT_A_STATUS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    private ResultActions postOrder(String token, Long themeId, Long packageId, LocalDate shootDate)
            throws Exception {
        return mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(orderBody(themeId, packageId, shootDate))));
    }

    private JsonNode createOrder(String token, Long themeId, Long packageId,
                                 LocalDate shootDate, String remark) throws Exception {
        JsonNode response = performJson(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(orderBody(themeId, packageId, shootDate, remark))), null);
        assertEquals(0, response.path("code").asInt(), response.toString());
        Long orderId = response.path("data").path("id").asLong();
        orderIds.add(orderId);
        return response.path("data");
    }

    private Map<String, Object> orderBody(Long themeId, Long packageId, LocalDate shootDate) {
        return orderBody(themeId, packageId, shootDate, null);
    }

    private Map<String, Object> orderBody(Long themeId, Long packageId, LocalDate shootDate, String remark) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("themeId", themeId);
        body.put("packageId", packageId);
        body.put("shootDate", shootDate);
        body.put("shootSlot", "MORNING");
        if (remark != null) {
            body.put("remark", remark);
        }
        return body;
    }

    private String adminToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return readBody(result).path("data").path("token").asText();
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

    private Long createTheme(String name, int status) {
        Theme theme = new Theme();
        theme.setName(name);
        theme.setSort(50);
        theme.setStatus(status);
        themeMapper.insert(theme);
        themeIds.add(theme.getId());
        return theme.getId();
    }

    private Long createPackage(Long themeId, BigDecimal price, int status) {
        Package pkg = new Package();
        pkg.setThemeId(themeId);
        pkg.setName("集成测试-订单套餐");
        pkg.setPrice(price);
        pkg.setRetouchCount(10);
        pkg.setSort(0);
        pkg.setStatus(status);
        packageMapper.insert(pkg);
        packageIds.add(pkg.getId());
        return pkg.getId();
    }

    private List<OrderLog> logsOf(Long orderId) {
        return orderLogMapper.selectList(new LambdaQueryWrapper<OrderLog>()
                .eq(OrderLog::getOrderId, orderId)
                .orderByAsc(OrderLog::getId));
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return readBody(result);
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private boolean listContains(JsonNode response, long id) {
        for (JsonNode node : response.path("data").path("list")) {
            if (node.path("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
