package com.localfood.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerProfileIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private String phone() { return "7" + (100000000 + new java.security.SecureRandom().nextInt(899999999)); }
    private String register(String phone) throws Exception {
        return JsonPathHelper.read(mvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("name", "Profile customer", "phone", phone, "password", "secret123"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }
    private ResultActions update(String token, String path, Map<String, Object> body) throws Exception {
        return mvc.perform(put("/api/v1/customers/me" + path).header("X-Customer-Token", token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }
    @Test void profileIsPrivateAndPhoneChangesRequirePasswordAndUniqueness() throws Exception {
        String oldPhone = phone(), newPhone = phone(), takenPhone = phone();
        String token = register(oldPhone), other = register(takenPhone);
        mvc.perform(get("/api/v1/customers/me").header("X-Customer-Token", "invalid")).andExpect(status().isBadRequest());
        update(token, "", Map.of("name", "Renamed", "phone", oldPhone)).andExpect(status().isOk());
        update(token, "", Map.of("name", "Renamed", "phone", newPhone)).andExpect(status().isBadRequest());
        update(token, "", Map.of("name", "Renamed", "phone", takenPhone, "currentPassword", "secret123")).andExpect(status().isBadRequest());
        update(token, "", Map.of("name", "Renamed", "phone", newPhone, "currentPassword", "secret123"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value(newPhone));
        mvc.perform(get("/api/v1/customers/me").header("X-Customer-Token", other)).andExpect(jsonPath("$.name").value("Profile customer"));
        mvc.perform(post("/api/v1/customers/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("phone", newPhone, "password", "secret123")))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/customers/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("phone", oldPhone, "password", "secret123")))).andExpect(status().isBadRequest());
    }
    @Test void savedAddressSupportsNotesAndPinWithCoordinateValidationAndOwnScope() throws Exception {
        String token = register(phone()), other = register(phone());
        update(token, "/address", Map.of("address", "12 Test Street", "latitude", 19.1)).andExpect(status().isBadRequest());
        update(token, "/address", Map.of("address", "12 Test Street", "latitude", 91, "longitude", 77.3)).andExpect(status().isBadRequest());
        update(token, "/address", Map.of("address", "12 Test Street", "notes", "Call at side gate", "latitude", 19.1, "longitude", 77.3))
            .andExpect(status().isOk()).andExpect(jsonPath("$.notes").value("Call at side gate"));
        mvc.perform(get("/api/v1/customers/me").header("X-Customer-Token", token)).andExpect(jsonPath("$.latitude").value(19.1));
        mvc.perform(get("/api/v1/customers/me").header("X-Customer-Token", other)).andExpect(jsonPath("$.address").isEmpty());
        update(token, "/address", Map.of("address", "New house with no pin")).andExpect(status().isOk()).andExpect(jsonPath("$.latitude").isEmpty());
    }
    @Test void etaRecalculatesFromLatestRiderPointAndExpiresWithoutFreshUpdates() {
        var order = new com.localfood.backend.order.CustomerOrder();
        var now = java.time.OffsetDateTime.now();
        order.setStatus(com.localfood.backend.order.OrderStatus.OUT_FOR_DELIVERY);
        order.setDispatchedAt(now); order.setDispatchLatitude(19.1); order.setDispatchLongitude(77.3);
        order.setDeliveryLatitude(19.15); order.setDeliveryLongitude(77.35);
        com.localfood.backend.delivery.DeliveryEstimate.calculate(order);
        double originalDistance = order.getEstimatedDistanceKm();
        order.setRiderLatitude(19.14); order.setRiderLongitude(77.34); order.setRiderLocationAt(now); order.setRiderSharing(true);
        com.localfood.backend.delivery.DeliveryEstimate.calculate(order);
        assertThat(order.getEstimatedDistanceKm()).isLessThan(originalDistance);
        assertThat(com.localfood.backend.delivery.DeliveryEstimateResponse.from(order).live()).isTrue();
        order.setRiderLocationAt(now.minusMinutes(1));
        assertThat(com.localfood.backend.delivery.DeliveryEstimateResponse.from(order).live()).isFalse();
        order.setStatus(com.localfood.backend.order.OrderStatus.DELIVERED);
        assertThat(com.localfood.backend.delivery.DeliveryEstimateResponse.from(order)).isNull();
    }
}
