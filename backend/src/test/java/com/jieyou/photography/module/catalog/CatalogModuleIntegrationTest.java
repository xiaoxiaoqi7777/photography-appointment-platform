package com.jieyou.photography.module.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jieyou.photography.module.catalog.entity.Package;
import com.jieyou.photography.module.catalog.entity.Theme;
import com.jieyou.photography.module.catalog.mapper.PackageMapper;
import com.jieyou.photography.module.catalog.mapper.ThemeMapper;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 主题/套餐模块集成测试（API 约定 5.2/5.3），使用本机 MySQL 的 photography 库。
 * 覆盖：管理端增改与上下架、用户端可见性、tags 数组与逗号分隔互转、套餐校验与关联主题下架提示。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    void adminCreatesThemeAndPublicCanViewIt() throws Exception {
        String token = adminToken();
        Long themeId = null;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", "集成测试-森系");
            body.put("coverUrl", "https://example.com/cover.jpg");
            body.put("description", "集成测试主题");
            body.put("tags", List.of("森系", "清新"));
            body.put("sort", 88);

            JsonNode created = createTheme(token, body);
            themeId = created.path("id").asLong();

            Theme saved = themeMapper.selectById(themeId);
            assertNotNull(saved);
            assertEquals("森系,清新", saved.getTags(), "tags 必须按逗号分隔持久化");
            assertEquals(88, saved.getSort());
            assertEquals(1, saved.getStatus(), "status 默认上架");

            mockMvc.perform(get("/api/themes/{id}", themeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("集成测试-森系"))
                    .andExpect(jsonPath("$.data.tags[0]").value("森系"))
                    .andExpect(jsonPath("$.data.tags[1]").value("清新"));

            mockMvc.perform(get("/api/themes").param("pageSize", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").isNumber());
            assertTrue(listContains(mockMvc.perform(get("/api/themes").param("pageSize", "100"))
                    .andReturn(), themeId), "用户端列表应包含上架主题");
        } finally {
            cleanupTheme(themeId);
        }
    }

    @Test
    void adminListSupportsStatusFilterAndPagination() throws Exception {
        String token = adminToken();
        Long onlineId = null;
        Long offlineId = null;
        try {
            onlineId = createTheme(token, Map.of("name", "集成测试-上架", "sort", 71)).path("id").asLong();
            offlineId = createTheme(token, Map.of("name", "集成测试-下架", "sort", 72, "status", 0))
                    .path("id").asLong();

            JsonNode offlinePage = performJson(get("/api/admin/themes")
                    .param("status", "0").param("page", "1").param("pageSize", "100"), token);
            assertTrue(listContains(offlinePage, offlineId), "状态筛选应包含下架主题");
            assertTrue(!listContains(offlinePage, onlineId), "状态筛选不应包含上架主题");
            assertEquals(1, offlinePage.path("data").path("page").asInt());
            assertEquals(100, offlinePage.path("data").path("pageSize").asInt());

            JsonNode publicPage = performJson(get("/api/themes").param("pageSize", "100"), null);
            assertTrue(listContains(publicPage, onlineId), "用户端列表应包含上架主题");
            assertTrue(!listContains(publicPage, offlineId), "用户端列表不应包含下架主题");
        } finally {
            cleanupTheme(onlineId);
            cleanupTheme(offlineId);
        }
    }

    @Test
    void editThemeOnlyUpdatesProvidedFields() throws Exception {
        String token = adminToken();
        Long themeId = null;
        try {
            themeId = createTheme(token, Map.of("name", "集成测试-原标题", "description", "原描述", "sort", 60))
                    .path("id").asLong();

            mockMvc.perform(put("/api/admin/themes/{id}", themeId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("name", "集成测试-新标题", "tags", List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("集成测试-新标题"))
                    .andExpect(jsonPath("$.data.tags").isEmpty())
                    .andExpect(jsonPath("$.data.description").value("原描述"));

            Theme saved = themeMapper.selectById(themeId);
            assertEquals("集成测试-新标题", saved.getName());
            assertNull(saved.getTags());

            mockMvc.perform(put("/api/admin/themes/{id}", themeId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        } finally {
            cleanupTheme(themeId);
        }
    }

    @Test
    void themeStatusChangesVisibility() throws Exception {
        String token = adminToken();
        Long themeId = null;
        try {
            themeId = createTheme(token, Map.of("name", "集成测试-上下架", "sort", 73)).path("id").asLong();

            mockMvc.perform(patch("/api/admin/themes/{id}/status", themeId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(0));

            mockMvc.perform(get("/api/themes/{id}", themeId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(40400));

            mockMvc.perform(get("/api/themes/{id}", themeId).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(0));

            JsonNode publicPage = performJson(get("/api/themes").param("pageSize", "100"), null);
            assertTrue(!listContains(publicPage, themeId), "下架后用户端列表不可见");

            mockMvc.perform(patch("/api/admin/themes/{id}/status", themeId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":2}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        } finally {
            cleanupTheme(themeId);
        }
    }

    @Test
    void userCannotAccessAdminCatalogApis() throws Exception {
        Long userId = createUser("it_catalog_user", 1);
        try {
            String token = jwtService.generateToken(userId, "it_catalog_user", "USER");
            mockMvc.perform(get("/api/admin/themes").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(40300));
            mockMvc.perform(get("/api/admin/packages").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(40300));
            mockMvc.perform(post("/api/admin/packages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(40300));
        } finally {
            userMapper.deleteById(userId);
        }
    }

    @Test
    void createPackageValidatesFields() throws Exception {
        String token = adminToken();
        Long themeId = null;
        Long packageId = null;
        try {
            themeId = createTheme(token, Map.of("name", "集成测试-套餐主题", "sort", 61)).path("id").asLong();

            postPackage(token, Map.of("themeId", 999999999L, "name", "无主题", "price", 100, "retouchCount", 1))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));

            postPackage(token, Map.of("themeId", themeId, "name", "零价格", "price", 0, "retouchCount", 1))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));

            postPackage(token, Map.of("themeId", themeId, "name", "零精修", "price", 100, "retouchCount", 0))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));

            postPackage(token, Map.of("themeId", themeId, "name", "三位小数", "price", new BigDecimal("12.345"),
                    "retouchCount", 1))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));

            JsonNode created = performJson(post("/api/admin/packages")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(packageBody(themeId, "集成测试-标准套餐", new BigDecimal("399.9"), 10))), null);
            packageId = created.path("data").path("id").asLong();
            assertEquals(0, created.path("code").asInt());
            assertEquals("成功", created.path("message").asText());

            Package saved = packageMapper.selectById(packageId);
            assertNotNull(saved);
            assertEquals(0, new BigDecimal("399.90").compareTo(saved.getPrice()));
            assertEquals(10, saved.getRetouchCount());
            assertEquals(0, saved.getSort());
            assertEquals(1, saved.getStatus());

            JsonNode userPackages = performJson(get("/api/themes/{id}/packages", themeId), null);
            assertTrue(listContainsArray(userPackages, packageId), "用户端应包含上架套餐");

            JsonNode adminOnline = performJson(get("/api/admin/packages")
                    .param("themeId", themeId.toString())
                    .param("status", "1")
                    .param("pageSize", "100"), token);
            assertTrue(listContains(adminOnline, packageId), "管理端按主题+状态筛选应命中上架套餐");

            mockMvc.perform(patch("/api/admin/packages/{id}/status", packageId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(0));

            assertTrue(!listContainsArray(performJson(get("/api/themes/{id}/packages", themeId), null), packageId),
                    "下架套餐不应出现在用户端列表");

            JsonNode adminOffline = performJson(get("/api/admin/packages")
                    .param("themeId", themeId.toString())
                    .param("status", "0")
                    .param("pageSize", "100"), token);
            assertTrue(listContains(adminOffline, packageId), "管理端筛选下架套餐应命中");

            mockMvc.perform(get("/api/packages/{id}", packageId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(40400));

            mockMvc.perform(get("/api/packages/{id}", packageId).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(0));
        } finally {
            if (packageId != null) {
                packageMapper.deleteById(packageId);
            }
            cleanupTheme(themeId);
        }
    }

    @Test
    void packageUnderOfflineThemeCanBeSavedWithWarning() throws Exception {
        String token = adminToken();
        Long themeId = null;
        Long packageId = null;
        try {
            themeId = createTheme(token, Map.of("name", "集成测试-下架主题", "sort", 62, "status", 0))
                    .path("id").asLong();

            JsonNode created = performJson(post("/api/admin/packages")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(packageBody(themeId, "下架主题套餐", new BigDecimal("199.00"), 5))), null);
            assertEquals(0, created.path("code").asInt(), "关联主题下架时仍允许保存");
            assertTrue(created.path("message").asText().contains("已下架"), "应返回关联主题下架提示");
            packageId = created.path("data").path("id").asLong();

            mockMvc.perform(get("/api/packages/{id}", packageId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(40400));

            mockMvc.perform(get("/api/themes/{id}/packages", themeId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(40400));

            mockMvc.perform(get("/api/packages/{id}", packageId).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/api/admin/packages/{id}", packageId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"改名套餐\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("改名套餐"))
                    .andExpect(jsonPath("$.message").value("保存成功，关联主题当前已下架"));

            mockMvc.perform(put("/api/admin/packages/{id}", packageId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"price\":-1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        } finally {
            if (packageId != null) {
                packageMapper.deleteById(packageId);
            }
            cleanupTheme(themeId);
        }
    }

    private String adminToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return readBody(result).path("data").path("token").asText();
    }

    private JsonNode createTheme(String token, Map<String, Object> body) throws Exception {
        JsonNode response = performJson(post("/api/admin/themes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)), null);
        assertEquals(0, response.path("code").asInt());
        return response.path("data");
    }

    private org.springframework.test.web.servlet.ResultActions postPackage(String token, Map<String, Object> body)
            throws Exception {
        return mockMvc.perform(post("/api/admin/packages")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private Map<String, Object> packageBody(Long themeId, String name, BigDecimal price, int retouchCount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("themeId", themeId);
        body.put("name", name);
        body.put("price", price);
        body.put("retouchCount", retouchCount);
        body.put("duration", "2 小时");
        body.put("includes", "精修与全部原片");
        return body;
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

    private boolean listContains(MvcResult result, long id) throws Exception {
        return listContains(readBody(result), id);
    }

    private boolean listContains(JsonNode response, long id) {
        for (JsonNode node : response.path("data").path("list")) {
            if (node.path("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    private boolean listContainsArray(JsonNode response, long id) {
        for (JsonNode node : response.path("data")) {
            if (node.path("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    private Long createUser(String username, int status) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("test123456"));
        user.setRole("USER");
        user.setStatus(status);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }

    private void cleanupTheme(Long themeId) {
        if (themeId == null) {
            return;
        }
        packageMapper.delete(new LambdaQueryWrapper<Package>().eq(Package::getThemeId, themeId));
        themeMapper.deleteById(themeId);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
