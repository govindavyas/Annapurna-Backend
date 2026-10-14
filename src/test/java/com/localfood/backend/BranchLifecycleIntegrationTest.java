package com.localfood.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localfood.backend.admin.AdminAccountRepository;
import com.localfood.backend.branch.BranchRepository;
import com.localfood.backend.branch.BranchService;
import com.localfood.backend.order.CustomerOrderRepository;
import com.localfood.backend.order.OrderStatus;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class BranchLifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BranchRepository branches;
    @Autowired BranchService branchService;
    @Autowired AdminAccountRepository accounts;
    @Autowired CustomerOrderRepository orders;
    @Autowired PlatformTransactionManager transactions;
    String branchId;
    String manager;
    String managerId;
    String courier;
    String courierId;
    String token;

    @BeforeEach
    void setup() throws Exception {
        String code = "LIFE-" + suffix();
        branchId = field(admin(post("/api/v1/admin/branches").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("code", code, "name", "Lifecycle test kitchen", "address", "Test address",
                "shopOpen", true, "deliveryRadiusKm", 5, "shopLatitude",18.52043,"shopLongitude",73.856743,"bannerTitle", "Test", "bannerSubtitle", "Test branch", "offerEnabled", false))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.shopOpen").value(false)), "id");
        manager = "life-manager-" + suffix(); courier = "life-courier-" + suffix();
        managerId = account(manager, "BRANCH_ADMIN");
        courierId = account(courier, "DELIVERY_STAFF");
        token = field(mvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("name", "Lifecycle customer", "phone", "9" + (100000000 + new java.security.SecureRandom().nextInt(899999999)), "password", "TestPass!123"))))
            .andExpect(status().isOk()), "token");
    }

    @Test
    void branchStartsClosedAndOnlyAcceptsOrdersBetweenStartAndClose() throws Exception {
        place().andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Selected branch is currently closed"));
        as(manager, acceptance(true)).andExpect(status().isOk()).andExpect(jsonPath("$.shopOpen").value(true));
        mvc.perform(get("/api/v1/features").param("branchId", branchId)).andExpect(jsonPath("$.shopOpen").value(true))
            .andExpect(jsonPath("$.paymentsEnabled").value(false));
        String id = field(place().andExpect(status().isOk()).andExpect(jsonPath("$.paymentMode").value("COD")), "orderId");
        as(manager, acceptance(false)).andExpect(status().isOk()).andExpect(jsonPath("$.shopOpen").value(false));
        place().andExpect(status().isBadRequest());
        as(manager, patch("/api/v1/admin/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void ordinarySettingsCannotAccidentallyReopenAClosedBranch() throws Exception {
        var settings = json.readTree(admin(get("/api/v1/admin/settings").param("branchId", branchId)).andReturn().getResponse().getContentAsString());
        ((com.fasterxml.jackson.databind.node.ObjectNode) settings).put("shopOpen", true);
        as(manager, put("/api/v1/admin/settings").param("branchId", branchId).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(settings))).andExpect(status().isOk()).andExpect(jsonPath("$.shopOpen").value(false));
        var branch = branches.findById(UUID.fromString(branchId)).orElseThrow();
        admin(put("/api/v1/admin/branches/{id}", branchId).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("code", branch.getCode(), "name", branch.getName(), "address", branch.getAddress(),
                "shopOpen", true, "deliveryRadiusKm", 5, "bannerTitle", "Updated banner", "bannerSubtitle", "Test", "offerEnabled", false))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.shopOpen").value(false));
    }

    @Test
    void onlyMainAdminDeletesAndBranchAdminCanOnlyStartOwnKitchen() throws Exception {
        as(courier, acceptance(true)).andExpect(status().isForbidden());
        as(manager, patch("/api/v1/admin/settings/accepting-orders").param("branchId", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON).content("{\"acceptingOrders\":true}")).andExpect(status().isForbidden());
        as(manager, delete("/api/v1/admin/branches/{id}", branchId)).andExpect(status().isForbidden());
        as(manager, delete("/api/v1/admin/accounts/{id}", courierId)).andExpect(status().isForbidden());
        as(courier, delete("/api/v1/admin/accounts/{id}", managerId)).andExpect(status().isForbidden());
    }

    @Test
    void deletingTeamRevokesAccessAndCannotBeUndoneWithEnable() throws Exception {
        admin(delete("/api/v1/admin/accounts/{id}", courierId)).andExpect(status().isNoContent());
        as(courier, get("/api/v1/admin/me")).andExpect(status().isUnauthorized());
        var listed = admin(get("/api/v1/admin/accounts")).andReturn().getResponse().getContentAsString();
        assertThat(listed).doesNotContain(courierId);
        admin(patch("/api/v1/admin/accounts/{id}/status", courierId).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
            .andExpect(status().isBadRequest());
        UUID ownId = accounts.findByUsernameIgnoreCase("admin").orElseThrow().getId();
        admin(delete("/api/v1/admin/accounts/{id}", ownId)).andExpect(status().isBadRequest());
        assertThat(accounts.findById(UUID.fromString(courierId)).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    void branchDeleteBlocksUnfinishedOrdersAndRetainsCompletedHistory() throws Exception {
        admin(acceptance(true)).andExpect(status().isOk());
        String id = field(place().andExpect(status().isOk()), "orderId");
        admin(delete("/api/v1/admin/branches/{id}", branchId)).andExpect(status().isBadRequest());
        assertThat(branches.findById(UUID.fromString(branchId)).orElseThrow().isDeleted()).isFalse();
        for (String state : List.of("ACCEPTED", "PREPARING", "READY_FOR_PICKUP")) {
            admin(patch("/api/v1/admin/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("status", state)))).andExpect(status().isOk());
        }
        as(courier, post("/api/v1/admin/delivery/orders/{id}/start", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"latitude\":19.1,\"longitude\":77.3,\"source\":\"GPS\"}")).andExpect(status().isOk());
        as(courier, post("/api/v1/admin/delivery/orders/{id}/complete", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"CASH\",\"confirmed\":true}")).andExpect(status().isOk());
        admin(delete("/api/v1/admin/branches/{id}", branchId)).andExpect(status().isNoContent());
        assertThat(mvc.perform(get("/api/v1/branches")).andReturn().getResponse().getContentAsString()).doesNotContain(branchId);
        assertThat(admin(get("/api/v1/admin/branches")).andReturn().getResponse().getContentAsString()).doesNotContain(branchId);
        assertThat(admin(get("/api/v1/admin/accounts")).andReturn().getResponse().getContentAsString()).doesNotContain(managerId, courierId);
        as(manager, get("/api/v1/admin/me")).andExpect(status().isUnauthorized());
        as(courier, get("/api/v1/admin/me")).andExpect(status().isUnauthorized());
        assertThat(orders.findById(UUID.fromString(id)).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(orders.findById(UUID.fromString(id)).orElseThrow().getCollectedBy()).isEqualTo(UUID.fromString(courierId));
        mvc.perform(get("/api/v1/orders/{id}", id).header("X-Customer-Token", token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.collectionMethod").value("CASH"));
        place().andExpect(status().isBadRequest());
        admin(acceptance(true)).andExpect(status().isBadRequest());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void deletingLastBranchLeavesAClosedStorefrontWithoutBreakingFeatures() throws Exception {
        branches.findAll().forEach(branch -> { branch.setDeleted(true); branch.setShopOpen(false); });
        branches.flush();
        mvc.perform(get("/api/v1/features").param("branchId", branchId)).andExpect(status().isOk())
            .andExpect(jsonPath("$.shopOpen").value(false)).andExpect(jsonPath("$.branches.length()").value(0));
        mvc.perform(post("/api/v1/orders").header("X-Customer-Token", token).contentType(MediaType.APPLICATION_JSON)
            .content(orderBody(false))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("All branches are currently closed"));
    }

    @Test
    void orderWaitingBehindCloseSeesCommittedClosedState() throws Exception {
        admin(acceptance(true)).andExpect(status().isOk());
        var started = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var future = new TransactionTemplate(transactions).execute(transaction -> {
                branchService.setAcceptingOrders(UUID.fromString(branchId), false);
                var request = executor.submit(() -> { started.countDown(); return place().andReturn().getResponse().getStatus(); });
                try { assertThat(started.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new RuntimeException(ex); }
                return request;
            });
            assertThat(future.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(400);
        }
        assertThat(orders.findByBranchIdOrderByCreatedAtDesc(UUID.fromString(branchId))).isEmpty();
    }

    private MockHttpServletRequestBuilder acceptance(boolean open) {
        return patch("/api/v1/admin/settings/accepting-orders").param("branchId", branchId)
            .contentType(MediaType.APPLICATION_JSON).content("{\"acceptingOrders\":" + open + "}");
    }
    private String account(String username, String role) throws Exception {
        return field(admin(post("/api/v1/admin/accounts").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("username", username, "role", role, "branchId", branchId,
                "password", "TestPass!123", "totpSecret", "654321")))).andExpect(status().isOk()), "id");
    }
    private byte[] orderBody(boolean scoped) throws Exception {
        var body = new java.util.HashMap<String, Object>(Map.of("customerName", "Lifecycle customer", "customerPhone", "9876543211",
            "deliveryAddress", "Test address", "deliveryLatitude",18.52043,"deliveryLongitude",73.856743,"paymentMode", "COD", "items", List.of(Map.of("menuItemId", "8fe9f1a6-3a09-43a0-b1f5-7e426cc271a1", "quantity", 1))));
        if (scoped) body.put("branchId", branchId);
        return json.writeValueAsBytes(body);
    }
    private ResultActions place() throws Exception {
        return mvc.perform(post("/api/v1/orders").header("X-Customer-Token", token).contentType(MediaType.APPLICATION_JSON).content(orderBody(true)));
    }
    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(httpBasic("admin", "admin-pass")).header("X-Admin-Totp", "654321"));
    }
    private ResultActions as(String username, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(httpBasic(username, "TestPass!123")).header("X-Admin-Totp", "654321"));
    }
    private String field(ResultActions result, String field) throws Exception {
        return JsonPathHelper.read(result.andReturn().getResponse().getContentAsString(), "$." + field);
    }
    private String suffix() { return UUID.randomUUID().toString().substring(0, 8); }
}
