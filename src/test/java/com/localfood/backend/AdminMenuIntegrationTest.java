package com.localfood.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMenuIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateUpdateUploadAndDeleteMenuItem() throws Exception {
        String create = """
            {
              "category":"Dinner",
              "name":"Pongal",
              "description":"Ghee pongal",
              "pricePaise":12000,
              "available":true,
              "imageUrl":"https://example.com/pongal.jpg"
            }
            """;

        MvcResult created = mockMvc.perform(post("/api/v1/admin/menu")
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321")
                .contentType(MediaType.APPLICATION_JSON)
                .content(create))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pongal"))
            .andReturn();

        String itemId = JsonPathHelper.read(created.getResponse().getContentAsString(), "$.id");

        String update = """
            {
              "category":"Dinner",
              "name":"Pongal Special",
              "description":"Ghee pongal with chutney",
              "pricePaise":13000,
              "available":false,
              "imageUrl":"https://example.com/pongal-new.jpg"
            }
            """;

        mockMvc.perform(put("/api/v1/admin/menu/{id}", itemId)
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321")
                .contentType(MediaType.APPLICATION_JSON)
                .content(update))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pongal Special"))
            .andExpect(jsonPath("$.available").value(false));

        MockMultipartFile image = new MockMultipartFile(
            "file",
            "pongal.png",
            "image/png",
            new byte[] {1, 2, 3, 4}
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/admin/menu/{id}/image", itemId)
                .file(image)
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.startsWith("/media/")));

        mockMvc.perform(delete("/api/v1/admin/menu/{id}/image", itemId)
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imageUrl").isEmpty());

        mockMvc.perform(delete("/api/v1/admin/menu/{id}", itemId)
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectMenuCreateWithoutSecondFactor() throws Exception {
        String create = """
            {
              "category":"Dinner",
              "name":"Uttapam",
              "description":"Onion uttapam",
              "pricePaise":10000,
              "available":true,
              "imageUrl":""
            }
            """;

        mockMvc.perform(post("/api/v1/admin/menu")
                .with(httpBasic("admin", "admin-pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(create))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldExposeCustomerInsightsForAdmin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/admin/settings/accepting-orders")
            .param("branchId", "11111111-1111-1111-1111-111111111111")
            .with(httpBasic("admin", "admin-pass")).header("X-Admin-Totp", "654321")
            .contentType(MediaType.APPLICATION_JSON).content("{\"acceptingOrders\":true}"))
            .andExpect(status().isOk());
        String registerPayload = """
            {
              "name":"Insights User",
              "phone":"9000000001",
              "password":"secret123"
            }
            """;

        MvcResult register = mockMvc.perform(post("/api/v1/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
            .andExpect(status().isOk())
            .andReturn();

        String token = JsonPathHelper.read(register.getResponse().getContentAsString(), "$.token");

        String orderPayload = """
            {
              "customerName":"Insights User",
              "customerPhone":"9000000001",
              "deliveryAddress":"Insight Street",
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
                .content(orderPayload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/customers/insights")
                .with(httpBasic("admin", "admin-pass"))
                .header("X-Admin-Totp", "654321"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Insights User"))
            .andExpect(jsonPath("$[0].phone").value("9000000001"))
            .andExpect(jsonPath("$[0].totalOrders").value(1))
            .andExpect(jsonPath("$[0].priorityTier").exists());
    }
}
