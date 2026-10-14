package com.localfood.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void startAcceptingOrders() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/settings/accepting-orders")
            .param("branchId", "11111111-1111-1111-1111-111111111111")
            .with(httpBasic("admin", "admin-pass")).header("X-Admin-Totp", "654321")
            .contentType(MediaType.APPLICATION_JSON).content("{\"acceptingOrders\":true}"))
            .andExpect(status().isOk());
    }

    private String registerAndGetToken(String name, String phone) throws Exception {
      String payload = """
        {
          "name":"%s",
          "phone":"%s",
          "password":"secret123"
        }
        """.formatted(name, phone);

      MvcResult register = mockMvc.perform(post("/api/v1/customers/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(payload))
        .andExpect(status().isOk())
        .andReturn();

      return JsonPathHelper.read(register.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void shouldPlaceCodOrderSuccessfully() throws Exception {
      String token = registerAndGetToken("Govind", "9999999999");

        String payload = """
            {
              "customerName":"Govind",
              "customerPhone":"9999999999",
              "deliveryAddress":"5km radius test address",
              "deliveryLatitude":18.52043,"deliveryLongitude":73.856743,
              "paymentMode":"COD",
              "items":[
                {"menuItemId":"8fe9f1a6-3a09-43a0-b1f5-7e426cc271a1", "quantity":1}
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
    .header("X-Customer-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PLACED"))
            .andExpect(jsonPath("$.paymentMode").value("COD"))
            .andExpect(jsonPath("$.paymentStatus").value("NOT_REQUIRED"));
    }

    @Test
    void shouldRejectOrderPlacementWithoutCustomerToken() throws Exception {
        String payload = """
            {
              "customerName":"Guest",
              "customerPhone":"9999999999",
              "deliveryAddress":"test address",
              "deliveryLatitude":18.52043,"deliveryLongitude":73.856743,
              "paymentMode":"COD",
              "items":[
                {"menuItemId":"8fe9f1a6-3a09-43a0-b1f5-7e426cc271a1", "quantity":1}
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Missing customer token"));
    }

    @Test
    void shouldRejectOnlinePaymentWhenFeatureDisabled() throws Exception {
      String token = registerAndGetToken("Govind Online", "9999999998");

        String payload = """
            {
              "customerName":"Govind",
              "customerPhone":"9999999999",
              "deliveryAddress":"test address",
              "deliveryLatitude":18.52043,"deliveryLongitude":73.856743,
              "paymentMode":"ONLINE",
              "items":[
                {"menuItemId":"63c3474a-96f6-4f85-bd14-c3d31f7b6ab6", "quantity":1}
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
    .header("X-Customer-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Online payments are currently disabled"));
    }

    @Test
    void shouldEnforceNoRefundCancellationAfterPreparing() throws Exception {
      String token = registerAndGetToken("Annapurna Kitchen", "9999999997");

        String payload = """
            {
              "customerName":"Govind",
              "customerPhone":"9999999999",
              "deliveryAddress":"test address",
              "deliveryLatitude":18.52043,"deliveryLongitude":73.856743,
              "paymentMode":"COD",
              "items":[
                {"menuItemId":"db03e13f-3195-4ca0-a111-4ff4b88d7980", "quantity":2}
              ]
            }
            """;

        MvcResult create = mockMvc.perform(post("/api/v1/orders")
    .header("X-Customer-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();

        String orderId = JsonPathHelper.read(create.getResponse().getContentAsString(), "$.orderId");

        mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/status", orderId)
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/status", orderId)
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/{orderId}/cancel", orderId)
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Customer changed mind\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Order cannot be cancelled after PREPARING stage as per policy"));
    }

        @Test
        void shouldNotifyCustomerWhenAdminRejectsOrder() throws Exception {
          String registerPayload = """
            {
              "name":"Tracking User",
              "phone":"9876543210",
              "password":"secret123"
            }
            """;

          MvcResult register = mockMvc.perform(post("/api/v1/customers/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(registerPayload))
            .andExpect(status().isOk())
            .andReturn();

          String customerToken = JsonPathHelper.read(register.getResponse().getContentAsString(), "$.token");

          String orderPayload = """
            {
              "customerName":"Tracking User",
              "customerPhone":"9876543210",
              "deliveryAddress":"Tracking Address",
              "deliveryLatitude":18.52043,"deliveryLongitude":73.856743,
              "paymentMode":"COD",
              "items":[
              {"menuItemId":"8fe9f1a6-3a09-43a0-b1f5-7e426cc271a1", "quantity":1}
              ]
            }
            """;

          MvcResult created = mockMvc.perform(post("/api/v1/orders")
              .header("X-Customer-Token", customerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(orderPayload))
            .andExpect(status().isOk())
            .andReturn();

          String orderId = JsonPathHelper.read(created.getResponse().getContentAsString(), "$.orderId");

          mockMvc.perform(post("/api/v1/admin/orders/{orderId}/reject", orderId)
              .with(httpBasic("admin", "admin-pass"))
              .header("X-Admin-Totp", "654321")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"reason\":\"Kitchen overloaded\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.rejectReason").value("Kitchen overloaded"));

          mockMvc.perform(get("/api/v1/customer/orders")
              .header("X-Customer-Token", customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].order.status").value("REJECTED"))
            .andExpect(jsonPath("$[0].notifications[0].message").value(org.hamcrest.Matchers.containsString("Order rejected")));
        }

        @Test
        void shouldAcceptPlus91PhoneAndInvalidateSessionOnLogout() throws Exception {
          String registerPayload = """
            {
              "name":"Phone Format User",
              "phone":"+91 99887-76655",
              "password":"secret123"
            }
            """;

          MvcResult register = mockMvc.perform(post("/api/v1/customers/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(registerPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value("9988776655"))
            .andReturn();

          String token = JsonPathHelper.read(register.getResponse().getContentAsString(), "$.token");

          mockMvc.perform(delete("/api/v1/customers/session")
              .header("X-Customer-Token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out successfully"));

          mockMvc.perform(get("/api/v1/customer/orders")
              .header("X-Customer-Token", token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid session"));
        }
}
