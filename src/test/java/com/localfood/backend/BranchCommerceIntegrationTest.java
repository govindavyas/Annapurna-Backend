package com.localfood.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.localfood.backend.branch.Branch;
import com.localfood.backend.branch.BranchRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BranchCommerceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BranchRepository branches;

    @Test void branchMenusRemainDiscoverableButCannotBeOrderedElsewhere() throws Exception {
        Branch a = branch(10, 10), b = branch(11, 11);
        String item = item("Breakfast", false, List.of(a.getId()));
        mvc.perform(get("/api/v1/menu")).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == '" + item + "')].branchIds[0]").value(a.getId().toString()));
        String token = customer();
        order(b, item, 1, token).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not served")));
        order(a, item, 1, token).andExpect(status().isOk()).andExpect(jsonPath("$.totalPaise").value(9000));
    }

    @Test void feesAndMinimumAreBranchSpecificAndExistingOrdersKeepTheirPrice() throws Exception {
        Branch a = branch(12, 12), b = branch(13, 13);
        settings(a, 4500, 12000).andExpect(status().isOk()).andExpect(jsonPath("$.deliveryFeePaise").value(4500));
        settings(b, 0, 0).andExpect(status().isOk());
        mvc.perform(get("/api/v1/features").param("branchId", a.getId().toString()))
            .andExpect(jsonPath("$.deliveryFeePaise").value(4500)).andExpect(jsonPath("$.minimumOrderPaise").value(12000))
            .andExpect(jsonPath("$.paymentsEnabled").value(false));
        String item = item("Lunch", true, List.of()), token = customer();
        order(a, item, 1, token).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Minimum food")));
        String orderId = field(order(a, item, 2, token).andExpect(status().isOk()).andExpect(jsonPath("$.totalPaise").value(16500)), "orderId");
        order(b, item, 1, token).andExpect(status().isOk()).andExpect(jsonPath("$.totalPaise").value(6000));
        settings(a, 0, 0).andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/{id}", orderId).header("X-Customer-Token", token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.deliveryFeePaise").value(4500));
    }

    @Test void invalidPricingAndBranchAssignmentsAreRejected() throws Exception {
        Branch a = branch(14, 14);
        settings(a, -1, 0).andExpect(status().isBadRequest());
        settings(a, 0, -1).andExpect(status().isBadRequest());
        menuRequest("Breakfast", false, List.of()).andExpect(status().isBadRequest());
        menuRequest("Breakfast", false, List.of(UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @Test void canonicalCategoriesAndLegacyAllBranchItemsArePreserved() throws Exception {
        Branch a = branch(15, 15);
        item("Evening Snacks", true, List.of());
        menuRequest("  evening   snacks ", true, List.of()).andExpect(status().isOk()).andExpect(jsonPath("$.category").value("Evening Snacks"));
        var result = admin(post("/api/v1/admin/menu").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of(
            "category", "Dinner", "name", "Legacy client dish", "description", "Fresh", "pricePaise", 6000, "available", true))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.allBranches").value(true));
        order(a, field(result, "id"), 1, customer()).andExpect(status().isOk());
    }

    @Test void nearestSelectionUsesCoverageAndRespectsExplicitChoice() throws Exception {
        Branch nearest = branch(-40, -100), serving = branch(-40.03, -100);
        nearest.setShopOpen(false); branches.saveAndFlush(nearest);
        mvc.perform(get("/api/v1/features").param("deliveryLatitude", "-40").param("deliveryLongitude", "-100"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.selectedBranchId").value(serving.getId().toString()));
        mvc.perform(get("/api/v1/features").param("branchId", nearest.getId().toString()).param("deliveryLatitude", "-40").param("deliveryLongitude", "-100"))
            .andExpect(jsonPath("$.selectedBranchId").value(nearest.getId().toString())).andExpect(jsonPath("$.shopOpen").value(false));
        serving.setShopOpen(false); branches.saveAndFlush(serving);
        mvc.perform(get("/api/v1/features").param("deliveryLatitude", "-40").param("deliveryLongitude", "-100"))
            .andExpect(jsonPath("$.selectedBranchId").value(nearest.getId().toString()));
        mvc.perform(get("/api/v1/features").param("deliveryLatitude", "91").param("deliveryLongitude", "0")).andExpect(status().isBadRequest());
    }

    @Test void branchAdminCanChangeOnlyOwnPricingAndCannotChangeCatalog() throws Exception {
        Branch a = branch(16, 16), b = branch(17, 17);
        String username = "pricing-" + UUID.randomUUID().toString().substring(0, 8);
        admin(post("/api/v1/admin/accounts").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of(
            "username", username, "password", "TestPass123!", "totpSecret", "123456", "role", "BRANCH_ADMIN", "branchId", a.getId(), "active", true))))
            .andExpect(status().isOk());
        ObjectNode body = settingsBody(a, 1234, 5000);
        mvc.perform(put("/api/v1/admin/settings").param("branchId", a.getId().toString()).with(httpBasic(username, "TestPass123!")).header("X-Admin-Totp", "123456")
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isOk()).andExpect(jsonPath("$.minimumOrderPaise").value(5000));
        mvc.perform(put("/api/v1/admin/settings").param("branchId", b.getId().toString()).with(httpBasic(username, "TestPass123!")).header("X-Admin-Totp", "123456")
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/menu").with(httpBasic(username, "TestPass123!")).header("X-Admin-Totp", "123456")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    private Branch branch(double lat, double lng) {
        Branch b = new Branch(); b.setCode("COM-" + UUID.randomUUID().toString().substring(0, 8)); b.setName(b.getCode()); b.setAddress("Test kitchen");
        b.setShopOpen(true); b.setDeliveryRadiusKm(5); b.setShopLatitude(lat); b.setShopLongitude(lng); b.setBannerTitle("Test"); b.setBannerSubtitle("Test kitchen");
        return branches.saveAndFlush(b);
    }
    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception { return mvc.perform(request.with(httpBasic("admin", "admin-pass")).header("X-Admin-Totp", "654321")); }
    private String field(ResultActions result, String name) throws Exception { return json.readTree(result.andReturn().getResponse().getContentAsString()).get(name).asText(); }
    private ResultActions menuRequest(String category, boolean all, List<UUID> ids) throws Exception {
        return admin(post("/api/v1/admin/menu").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of(
            "category", category, "name", "Branch dish", "description", "Fresh", "pricePaise", 6000, "available", true, "allBranches", all, "branchIds", ids))));
    }
    private String item(String category, boolean all, List<UUID> ids) throws Exception { return field(menuRequest(category, all, ids).andExpect(status().isOk()), "id"); }
    private ObjectNode settingsBody(Branch branch, int fee, int minimum) throws Exception {
        ObjectNode body = (ObjectNode) json.readTree(admin(get("/api/v1/admin/settings").param("branchId", branch.getId().toString())).andReturn().getResponse().getContentAsString());
        body.put("deliveryFeePaise", fee); body.put("minimumOrderPaise", minimum); return body;
    }
    private ResultActions settings(Branch branch, int fee, int minimum) throws Exception {
        return admin(put("/api/v1/admin/settings").param("branchId", branch.getId().toString()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(settingsBody(branch, fee, minimum))));
    }
    private String customer() throws Exception {
        return field(mvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of(
            "name", "Commerce test", "phone", "9" + String.format("%09d", new java.security.SecureRandom().nextInt(1000000000)), "password", "Customer123!")))).andExpect(status().isOk()), "token");
    }
    private ResultActions order(Branch branch, String item, int quantity, String token) throws Exception {
        return mvc.perform(post("/api/v1/orders").header("X-Customer-Token", token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of(
            "branchId", branch.getId(), "customerName", "Commerce test", "customerPhone", "9000000000", "deliveryAddress", "Test street", "deliveryLatitude",branch.getShopLatitude(),"deliveryLongitude",branch.getShopLongitude(),"paymentMode", "COD", "items", List.of(Map.of("menuItemId", item, "quantity", quantity))))));
    }
}
