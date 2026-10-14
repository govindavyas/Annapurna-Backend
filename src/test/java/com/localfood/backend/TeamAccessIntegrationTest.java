package com.localfood.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localfood.backend.admin.AdminAccountRepository;
import com.localfood.backend.branch.Branch;
import com.localfood.backend.branch.BranchRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class TeamAccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BranchRepository branches;
    @Autowired AdminAccountRepository accounts;
    UUID first;
    UUID second;
    String manager;
    String courier;
    String managerId;
    String courierId;

    @BeforeEach
    void setup() throws Exception {
        first = branch();
        second = branch();
        manager = "manager-" + suffix();
        courier = "courier-" + suffix();
        managerId = field(create(manager, "BRANCH_ADMIN", first, "TestPass!123", "654321").andExpect(status().isOk()), "id");
        courierId = field(create(courier, "DELIVERY_STAFF", first, "TestPass!123", "654321").andExpect(status().isOk()), "id");
    }

    @Test
    void createsEachRoleAndSignsInWithAssignedScope() throws Exception {
        as(manager, get("/api/v1/admin/me")).andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("BRANCH_ADMIN")).andExpect(jsonPath("$.branchId").value(first.toString()));
        as(courier, get("/api/v1/admin/me")).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("DELIVERY_STAFF"));
        String owner = "owner-" + suffix();
        create(owner, "SUPER_ADMIN", null, "TestPass!123", "654321").andExpect(status().isOk());
        as(owner, get("/api/v1/admin/accounts")).andExpect(status().isOk());
        as(manager, get("/api/v1/admin/branches")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(first.toString()));
    }

    @Test
    void reportsCredentialFieldErrorsAndRejectsCaseInsensitiveDuplicates() throws Exception {
        create("bad:user", "BRANCH_ADMIN", first, "short", "123").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[*].field", hasItems("username", "password", "totpSecret")));
        create("unicode-" + suffix(), "BRANCH_ADMIN", first, "界".repeat(25), "654321")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Password must not exceed 72 UTF-8 bytes. Use a shorter password"));
        create(manager.toUpperCase(), "BRANCH_ADMIN", first, "TestPass!123", "654321")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Username already exists. Choose a different username"));
        create("missing-" + suffix(), "BRANCH_ADMIN", null, "TestPass!123", "654321").andExpect(status().isBadRequest());
        create("wrong-" + suffix(), "SUPER_ADMIN", first, "TestPass!123", "654321").andExpect(status().isBadRequest());
    }

    @Test
    void supportsUtf8PasswordsWithoutBreakingBasicAuthentication() throws Exception {
        String name = "unicode-" + suffix();
        String password = "SafePassword-नमस्ते";
        create(name.toUpperCase(), "BRANCH_ADMIN", first, password, "654321").andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(name));
        String header = "Basic " + Base64.getEncoder().encodeToString((name + ":" + password).getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/v1/admin/me").header("Authorization", header).header("X-Admin-Totp", "654321"))
            .andExpect(status().isOk());
    }

    @Test
    void branchAdminCannotUseGlobalOrOtherBranchEndpoints() throws Exception {
        for (String path : List.of("accounts", "branches/" + first + "/accounts")) {
            as(manager, get("/api/v1/admin/" + path)).andExpect(status().isForbidden());
        }
        as(manager, post("/api/v1/admin/accounts").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        as(manager, patch("/api/v1/admin/accounts/{id}/access", courierId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"SUPER_ADMIN\"}")).andExpect(status().isForbidden());
        as(manager, patch("/api/v1/admin/accounts/{id}/status", courierId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"active\":false}")).andExpect(status().isForbidden());
        as(manager, post("/api/v1/admin/branches").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        as(manager, put("/api/v1/admin/branches/{id}", first).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        for (var request : List.of(post("/api/v1/admin/menu"), put("/api/v1/admin/menu/{id}", UUID.randomUUID()),
            delete("/api/v1/admin/menu/{id}", UUID.randomUUID()), post("/api/v1/admin/menu/{id}/image", UUID.randomUUID()))) {
            as(manager, request).andExpect(status().isForbidden());
        }
        for (String path : List.of("orders", "settings", "customers/insights")) {
            as(manager, get("/api/v1/admin/" + path).param("branchId", first.toString())).andExpect(status().isOk());
            as(manager, get("/api/v1/admin/" + path).param("branchId", second.toString())).andExpect(status().isForbidden());
        }
        as(courier, get("/api/v1/admin/branches")).andExpect(status().isForbidden());
        as(courier, get("/api/v1/admin/customers/insights")).andExpect(status().isForbidden());
        as(courier, patch("/api/v1/admin/accounts/{id}/access", courierId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"SUPER_ADMIN\"}")).andExpect(status().isForbidden());
    }

    @Test
    void branchAdminCanSaveOwnSettingsButCannotWriteOtherBranch() throws Exception {
        var response = as(manager, get("/api/v1/admin/settings")).andExpect(status().isOk()).andReturn();
        var settings = json.readTree(response.getResponse().getContentAsString());
        ((com.fasterxml.jackson.databind.node.ObjectNode) settings).put("shopName", "Updated own test kitchen");
        as(manager, put("/api/v1/admin/settings").param("branchId", first.toString()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(settings))).andExpect(status().isOk()).andExpect(jsonPath("$.shopName").value("Updated own test kitchen"));
        as(manager, put("/api/v1/admin/settings").param("branchId", second.toString()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(settings))).andExpect(status().isForbidden());
        assertThat(branches.findById(second).orElseThrow().getName()).isEqualTo("Team test kitchen");
    }

    @Test
    void accessAndStatusChangesTakeEffectOnNextRequest() throws Exception {
        admin(patch("/api/v1/admin/accounts/{id}/access", managerId).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("role", "DELIVERY_STAFF", "branchId", second))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("DELIVERY_STAFF"));
        as(manager, get("/api/v1/admin/orders")).andExpect(status().isForbidden());
        as(manager, get("/api/v1/admin/delivery/payment-settings")).andExpect(status().isOk())
            .andExpect(jsonPath("$.branchId").value(second.toString()));
        as(manager, get("/api/v1/admin/delivery/payment-settings").param("branchId", first.toString())).andExpect(status().isBadRequest());
        admin(patch("/api/v1/admin/accounts/{id}/status", managerId).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
            .andExpect(status().isOk());
        as(manager, get("/api/v1/admin/me")).andExpect(status().isUnauthorized());
        admin(patch("/api/v1/admin/accounts/{id}/status", managerId).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
            .andExpect(status().isOk());
        as(manager, get("/api/v1/admin/me")).andExpect(status().isOk());
    }

    @Test
    void preventsSelfLockoutAndInvalidScopeChanges() throws Exception {
        UUID owner = accounts.findByUsernameIgnoreCase("admin").orElseThrow().getId();
        admin(patch("/api/v1/admin/accounts/{id}/status", owner).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
            .andExpect(status().isBadRequest());
        admin(patch("/api/v1/admin/accounts/{id}/access", owner).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("role", "BRANCH_ADMIN", "branchId", first))))
            .andExpect(status().isBadRequest());
        admin(patch("/api/v1/admin/accounts/{id}/access", courierId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"BRANCH_ADMIN\"}")).andExpect(status().isBadRequest());
        as(courier, get("/api/v1/admin/me")).andExpect(jsonPath("$.role").value("DELIVERY_STAFF"));
    }

    @Test
    void concurrentDuplicateCreateReturnsOneAccountAndOneConflict() throws Exception {
        String username = "duplicate-" + suffix();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(
                () -> create(username, "BRANCH_ADMIN", first, "TestPass!123", "654321").andReturn().getResponse().getStatus(),
                () -> create(username.toUpperCase(), "BRANCH_ADMIN", first, "TestPass!123", "654321").andReturn().getResponse().getStatus()));
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 409);
        }
    }

    @Test
    void legacyOrderDetailRouteRequiresTheOwningCustomer() throws Exception {
        String token = customerToken();
        String id = field(mvc.perform(post("/api/v1/orders").header("X-Customer-Token", token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("branchId", first, "customerName", "Privacy test", "customerPhone", "9876543211",
                "deliveryAddress", "Test address", "deliveryLatitude",18.52043,"deliveryLongitude",73.856743,"paymentMode", "COD", "items", List.of(Map.of("menuItemId", "8fe9f1a6-3a09-43a0-b1f5-7e426cc271a1", "quantity", 1))))))
            .andExpect(status().isOk()), "orderId");
        mvc.perform(get("/api/v1/orders/{id}", id)).andExpect(status().isBadRequest());
        as(courier, get("/api/v1/orders/{id}", id)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/orders/{id}", id).header("X-Customer-Token", customerToken())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/orders/{id}", id).header("X-Customer-Token", token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(id));
        as(manager, patch("/api/v1/admin/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
            .andExpect(status().isOk());
    }

    private ResultActions create(String name, String role, UUID branch, String password, String code) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("username", name); body.put("role", role); body.put("branchId", branch); body.put("password", password); body.put("totpSecret", code);
        return admin(post("/api/v1/admin/accounts").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }
    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(httpBasic("admin", "admin-pass")).header("X-Admin-Totp", "654321"));
    }
    private ResultActions as(String name, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(httpBasic(name, "TestPass!123")).header("X-Admin-Totp", "654321"));
    }
    private String field(ResultActions result, String field) throws Exception {
        return JsonPathHelper.read(result.andReturn().getResponse().getContentAsString(), "$." + field);
    }
    private UUID branch() {
        var branch = new Branch();
        branch.setCode("T-" + UUID.randomUUID()); branch.setName("Team test kitchen"); branch.setAddress("Test kitchen address");
        branch.setShopLatitude(18.52043); branch.setShopLongitude(73.856743);
        branch.setShopOpen(true); branch.setDeliveryRadiusKm(5); branch.setBannerTitle("Test kitchen"); branch.setBannerSubtitle("Test branch");
        return branches.save(branch).getId();
    }
    private String customerToken() throws Exception {
        return field(mvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("name", "Privacy test", "phone", "9" + (100000000 + new java.security.SecureRandom().nextInt(899999999)), "password", "TestPass!123"))))
            .andExpect(status().isOk()), "token");
    }
    private String suffix() { return UUID.randomUUID().toString().substring(0, 8); }
}
