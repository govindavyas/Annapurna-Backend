package com.localfood.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.localfood.backend.admin.AdminAccountRepository;
import com.localfood.backend.branch.Branch;
import com.localfood.backend.branch.BranchRepository;
import com.localfood.backend.order.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BranchRepository branches;
    @Autowired CustomerOrderRepository orders;
    @Autowired OrderEventRepository events;
    @Autowired AdminAccountRepository accounts;

    UUID branchId;
    String username;
    String token;

    @BeforeEach
    void setup() throws Exception {
        branchId = createBranch();
        username = "delivery-" + UUID.randomUUID().toString().substring(0, 8);
        createAccount(username, "DELIVERY_STAFF", branchId);
        String phone = "9" + (100000000 + new java.security.SecureRandom().nextInt(899999999));
        token = field(mvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON)
            .content(""" 
                {"name":"Delivery test customer","phone":"%s","password":"secret123"}
                """.formatted(phone))).andExpect(status().isOk()), "token");
    }

    @Test
    void cashCollectionCompletesCodOrderAndRetriesWithoutDuplicateEvent() throws Exception {
        String id = createOrder(branchId, true);
        staff(complete(id, "CASH", null)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DELIVERED"))
            .andExpect(jsonPath("$.collectionMethod").value("CASH"))
            .andExpect(jsonPath("$.collectedAt").isNotEmpty());
        staff(complete(id, "CASH", null)).andExpect(status().isOk());
        CustomerOrder saved = orders.findById(UUID.fromString(id)).orElseThrow();
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.COLLECTED);
        assertThat(saved.getCollectedBy()).isNotNull();
        assertThat(events.findByOrderIdOrderByCreatedAtDesc(saved.getId()).stream()
            .filter(event -> event.getStatus() == OrderStatus.DELIVERED).count()).isEqualTo(1);
        staff(get("/api/v1/admin/delivery/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/customer/orders/{id}", id).header("X-Customer-Token", token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.order.collectionMethod").value("CASH"));
    }

    @Test
    void upiRequiresCurrentAdminQrAndManualConfirmationWithPaymentsDisabled() throws Exception {
        String id = createOrder(branchId, true);
        staff(complete(id, "UPI", null)).andExpect(status().isBadRequest());
        String qr = uploadQr(branchId);
        staff(get("/api/v1/admin/delivery/payment-settings"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.qrImageUrl").value(qr));
        staff(complete(id, "UPI", "/media/old-qr.png")).andExpect(status().isBadRequest());
        staff(post("/api/v1/admin/delivery/orders/{id}/complete", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"UPI\",\"confirmed\":false,\"qrImageUrl\":\"" + qr + "\"}"))
            .andExpect(status().isBadRequest());
        staff(complete(id, "UPI", qr)).andExpect(status().isOk())
            .andExpect(jsonPath("$.collectionMethod").value("UPI"));
        assertThat(orders.findById(UUID.fromString(id)).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.COLLECTED);
        mvc.perform(get("/api/v1/features")).andExpect(jsonPath("$.paymentsEnabled").value(false));
    }

    @Test
    void staffCanOnlyReadBranchDeliveriesAndCannotUseAdminTools() throws Exception {
        createOrder(branchId, true);
        createOrder(branchId, false);
        UUID otherBranch = createBranch();
        String otherOrder = createOrder(otherBranch, true);
        staff(get("/api/v1/admin/me")).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("DELIVERY_STAFF"));
        staff(get("/api/v1/admin/delivery/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        for (String path : List.of("orders", "menu", "accounts", "branches", "settings", "customers/insights", "delivery-payment")) {
            staff(get("/api/v1/admin/" + path)).andExpect(status().isForbidden());
        }
        staff(get("/api/v1/admin/delivery/orders").param("branchId", otherBranch.toString())).andExpect(status().isBadRequest());
        staff(complete(otherOrder, "CASH", null)).andExpect(status().isBadRequest());
        staff(patch("/api/v1/admin/orders/{id}/status", otherOrder).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"DELIVERED\"}")).andExpect(status().isForbidden());
        staff(delete("/api/v1/admin/delivery-payment/qr")).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/admin/delivery-payment/qr").file(png())
            .with(httpBasic(username, "secret123")).header("X-Admin-Totp", "123456"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/delivery/orders").with(httpBasic(username, "secret123")))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/delivery/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void unreadyOrdersCannotBeCollectedAndAdminCannotBypassCollection() throws Exception {
        String unready = createOrder(branchId, false);
        staff(complete(unready, "CASH", null)).andExpect(status().isBadRequest());
        String ready = createOrder(branchId, true);
        admin(patch("/api/v1/admin/orders/{id}/status", ready).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"DELIVERED\"}")).andExpect(status().isBadRequest());
        staff(post("/api/v1/admin/delivery/orders/{id}/complete", ready).contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"CASH\"}")).andExpect(status().isBadRequest());
        assertThat(orders.findById(UUID.fromString(ready)).orElseThrow().getCollectedAt()).isNull();
        admin(complete(ready, "CASH", null)).andExpect(status().isOk());
        staff(complete(ready, "CASH", null)).andExpect(status().isBadRequest());
    }

    @Test
    void qrValidationRemovalAndBranchAdminScopeAreEnforced() throws Exception {
        mvc.perform(multipart("/api/v1/admin/delivery-payment/qr").file(new MockMultipartFile("file", "bad.png", "image/png", new byte[]{1, 2}))
            .param("branchId", branchId.toString()).with(httpBasic("admin", "admin-pass")).header("X-Admin-Totp", "654321"))
            .andExpect(status().isBadRequest());
        uploadQr(branchId);
        admin(delete("/api/v1/admin/delivery-payment/qr").param("branchId", branchId.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.qrImageUrl").isEmpty());
        staff(complete(createOrder(branchId, true), "UPI", "/media/deleted.png")).andExpect(status().isBadRequest());
        String branchAdmin = "branch-" + UUID.randomUUID().toString().substring(0, 8);
        createAccount(branchAdmin, "BRANCH_ADMIN", branchId);
        mvc.perform(get("/api/v1/admin/delivery-payment").with(httpBasic(branchAdmin, "secret123"))
            .header("X-Admin-Totp", "123456")).andExpect(status().isOk());
        mvc.perform(multipart("/api/v1/admin/delivery-payment/qr").file(png()).param("branchId", createBranch().toString())
            .with(httpBasic(branchAdmin, "secret123")).header("X-Admin-Totp", "123456"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deliveryAccountsRequireBranchAndDisabledStaffCannotLogIn() throws Exception {
        admin(post("/api/v1/admin/accounts").contentType(MediaType.APPLICATION_JSON).content("""
            {"username":"unscoped-delivery","password":"secret123","totpSecret":"123456","role":"DELIVERY_STAFF"}
            """)).andExpect(status().isBadRequest());
        var account = accounts.findByUsernameIgnoreCase(username).orElseThrow();
        admin(patch("/api/v1/admin/accounts/{id}/status", account.getId()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"active\":false}")).andExpect(status().isOk());
        staff(get("/api/v1/admin/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentDeliveryRetriesRecordOneCollection() throws Exception {
        String id = createOrder(branchId, true);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(
                () -> staff(complete(id, "CASH", null)).andReturn().getResponse().getStatus(),
                () -> staff(complete(id, "CASH", null)).andReturn().getResponse().getStatus()));
            for (var result : results) assertThat(result.get()).isEqualTo(200);
        }
        assertThat(events.findByOrderIdOrderByCreatedAtDesc(UUID.fromString(id)).stream()
            .filter(event -> event.getStatus() == OrderStatus.DELIVERED).count()).isEqualTo(1);
    }


    @Test
    void savedAddressEditsDoNotRedirectAnExistingOrder() throws Exception {
        String id = createOrder(branchId, false);
        mvc.perform(put("/api/v1/customers/me/address").header("X-Customer-Token", token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"address\":\"A different house\",\"notes\":\"New gate\",\"latitude\":19.2,\"longitude\":77.4}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/{id}", id).header("X-Customer-Token", token))
            .andExpect(jsonPath("$.deliveryAddress").value("12 Test Street, second floor"))
            .andExpect(jsonPath("$.deliveryNotes").value("Side entrance"))
            .andExpect(jsonPath("$.deliveryLatitude").value(19.12));
    }

    @Test
    void acceptanceRevealsDestinationOnlyToWinningRiderAndCannotBeBypassed() throws Exception {
        String id = readyOrder();
        staff(get("/api/v1/admin/delivery/orders")).andExpect(jsonPath("$[0].deliveryAddress").isEmpty())
            .andExpect(jsonPath("$[0].customerPhone").isEmpty());
        staff(complete(id, "CASH", null)).andExpect(status().isBadRequest());
        admin(patch("/api/v1/admin/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"OUT_FOR_DELIVERY\"}")).andExpect(status().isBadRequest());
        String other = "rider-" + UUID.randomUUID().toString().substring(0, 8);
        createAccount(other, "DELIVERY_STAFF", branchId);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(
                () -> staff(start(id)).andReturn().getResponse().getStatus(),
                () -> mvc.perform(start(id).with(httpBasic(other, "secret123")).header("X-Admin-Totp", "123456")).andReturn().getResponse().getStatus()));
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 400);
        }
        var saved = orders.findById(UUID.fromString(id)).orElseThrow();
        String winner = accounts.findById(saved.getDispatchedBy()).orElseThrow().getUsername();
        String loser = winner.equals(username) ? other : username;
        mvc.perform(start(id).with(httpBasic(winner, "secret123")).header("X-Admin-Totp", "123456"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.deliveryAddress").value("12 Test Street, second floor"));
        assertThat(orders.findById(saved.getId()).orElseThrow().getDispatchedAt()).isEqualTo(saved.getDispatchedAt());
        assertThat(events.findByOrderIdOrderByCreatedAtDesc(saved.getId()).stream().filter(e -> e.getStatus() == OrderStatus.OUT_FOR_DELIVERY).count()).isEqualTo(1);
        mvc.perform(get("/api/v1/admin/delivery/orders").with(httpBasic(loser, "secret123")).header("X-Admin-Totp", "123456"))
            .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(complete(id, "CASH", null).with(httpBasic(loser, "secret123")).header("X-Admin-Totp", "123456"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void liveLocationIsOwnedFreshAndStopsAfterDeliveryWithoutCreatingTimelineNoise() throws Exception {
        String id = readyOrder();
        staff(start(id)).andExpect(status().isOk());
        int eventCount = events.findByOrderIdOrderByCreatedAtDesc(UUID.fromString(id)).size();
        var now = java.time.OffsetDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        admin(location(id, now)).andExpect(status().isBadRequest());
        staff(location(id, now.minusMinutes(2))).andExpect(status().isBadRequest());
        staff(location(id, now.plusMinutes(2))).andExpect(status().isBadRequest());
        staff(location(id, now)).andExpect(status().isOk()).andExpect(jsonPath("$.deliveryEstimate.live").value(true));
        mvc.perform(get("/api/v1/orders/{id}", id).header("X-Customer-Token", token))
            .andExpect(jsonPath("$.deliveryEstimate.latitude").value(19.11));
        String otherToken = field(mvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Other customer\",\"phone\":\"8%s\",\"password\":\"secret123\"}".formatted(100000000 + new java.security.SecureRandom().nextInt(899999999))))
            .andExpect(status().isOk()), "token");
        mvc.perform(get("/api/v1/orders/{id}", id).header("X-Customer-Token", otherToken)).andExpect(status().isBadRequest());
        staff(location(id, now.minusSeconds(1))).andExpect(status().isOk());
        assertThat(orders.findById(UUID.fromString(id)).orElseThrow().getRiderLocationAt()).isEqualTo(now);
        assertThat(events.findByOrderIdOrderByCreatedAtDesc(UUID.fromString(id))).hasSize(eventCount);
        staff(post("/api/v1/admin/delivery/orders/{id}/location/pause", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/orders/{id}", id).header("X-Customer-Token", token))
            .andExpect(jsonPath("$.deliveryEstimate.live").value(false));
        staff(complete(id, "CASH", null)).andExpect(status().isOk()).andExpect(jsonPath("$.deliveryEstimate").isEmpty());
        staff(location(id, java.time.OffsetDateTime.now())).andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder location(String id, java.time.OffsetDateTime observed) {
        return put("/api/v1/admin/delivery/orders/{id}/location", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"latitude\":19.11,\"longitude\":77.31,\"accuracyMeters\":15,\"observedAt\":\"%s\"}".formatted(observed));
    }
    private String readyOrder() throws Exception {
        String id = createOrder(branchId, false);
        for (String stage : List.of("ACCEPTED", "PREPARING", "READY_FOR_PICKUP"))
            admin(patch("/api/v1/admin/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + stage + "\"}")).andExpect(status().isOk());
        return id;
    }

    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(httpBasic("admin", "admin-pass")).header("X-Admin-Totp", "654321"));
    }

    private ResultActions staff(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(httpBasic(username, "secret123")).header("X-Admin-Totp", "123456"));
    }

    private String field(ResultActions result, String field) throws Exception {
        return JsonPathHelper.read(result.andReturn().getResponse().getContentAsString(), "$." + field);
    }

    private void createAccount(String name, String role, UUID branch) throws Exception {
        admin(post("/api/v1/admin/accounts").contentType(MediaType.APPLICATION_JSON).content("""
            {"username":"%s","password":"secret123","totpSecret":"123456","role":"%s","branchId":"%s"}
            """.formatted(name, role, branch))).andExpect(status().isOk());
    }

    private UUID createBranch() {
        Branch branch = new Branch();
        branch.setCode("T-" + UUID.randomUUID());
        branch.setName("Delivery test kitchen");
        branch.setAddress("Test kitchen address");
        branch.setShopOpen(true);
        branch.setShopLatitude(19.12); branch.setShopLongitude(77.32);
        branch.setDeliveryRadiusKm(5);
        branch.setBannerTitle("Test kitchen");
        branch.setBannerSubtitle("Test deliveries");
        return branches.save(branch).getId();
    }

    private String createOrder(UUID branch, boolean dispatch) throws Exception {
        String id = field(mvc.perform(post("/api/v1/orders").header("X-Customer-Token", token)
            .contentType(MediaType.APPLICATION_JSON).content("""
            {"branchId":"%s","customerName":"Delivery test","customerPhone":"9876543201",
             "deliveryAddress":"12 Test Street, second floor","deliveryNotes":"Side entrance","deliveryLatitude":19.12,"deliveryLongitude":77.32,"paymentMode":"COD",
             "items":[{"menuItemId":"8fe9f1a6-3a09-43a0-b1f5-7e426cc271a1","quantity":1}]}
            """.formatted(branch))).andExpect(status().isOk()), "orderId");
        if (dispatch) for (String stage : List.of("ACCEPTED", "PREPARING", "READY_FOR_PICKUP")) {
            admin(patch("/api/v1/admin/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + stage + "\"}")).andExpect(status().isOk());
        }
        if (dispatch) {
            var request = start(id);
            (branch.equals(branchId) ? staff(request) : admin(request)).andExpect(status().isOk());
        }
        return id;
    }

    private MockHttpServletRequestBuilder start(String id) {
        return post("/api/v1/admin/delivery/orders/{id}/start", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"latitude\":19.1,\"longitude\":77.3,\"source\":\"GPS\"}");
    }

    private MockHttpServletRequestBuilder complete(String id, String method, String qr) {
        return post("/api/v1/admin/delivery/orders/{id}/complete", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"" + method + "\",\"confirmed\":true,\"qrImageUrl\":" + (qr == null ? "null" : "\"" + qr + "\"") + "}");
    }

    private MockMultipartFile png() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "qr.png", "image/png", output.toByteArray());
    }

    private String uploadQr(UUID branch) throws Exception {
        return field(mvc.perform(multipart("/api/v1/admin/delivery-payment/qr").file(png()).param("branchId", branch.toString())
            .with(httpBasic("admin", "admin-pass")).header("X-Admin-Totp", "654321"))
            .andExpect(status().isOk()), "qrImageUrl");
    }
}
