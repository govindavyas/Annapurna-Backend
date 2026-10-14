package com.localfood.backend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localfood.backend.branch.*;
import com.localfood.backend.menu.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class CategoryAndRangeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BranchRepository branches;
    @Autowired MenuItemRepository items;
    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception { return mvc.perform(request.with(httpBasic("admin","admin-pass")).header("X-Admin-Totp","654321")); }
    private MockHttpServletRequestBuilder category(String name) { return post("/api/v1/admin/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+name+"\"}"); }
    @Test void categoryCrudCanonicalRenameAndUsedDeletionAreSafe() throws Exception {
        String name="Test category "+UUID.randomUUID().toString().substring(0,8);
        admin(category(name)).andExpect(status().isOk()).andExpect(jsonPath("$.productCount").value(0));
        admin(category("  "+name.toUpperCase()+"  ")).andExpect(status().isBadRequest());
        admin(put("/api/v1/admin/categories").param("name",name).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+name.toUpperCase()+"\"}"))
            .andExpect(status().isOk());
        name=name.toUpperCase();
        var item=items.findAll().get(0); String original=item.getCategory();
        admin(put("/api/v1/admin/categories").param("name",original).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+name+"\"}"))
            .andExpect(status().isBadRequest());
        String renamed="Renamed "+UUID.randomUUID().toString().substring(0,8);
        admin(put("/api/v1/admin/categories").param("name",original).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+renamed+"\"}"))
            .andExpect(status().isOk());
        assertThat(items.findById(item.getId()).orElseThrow().getCategory()).isEqualTo(renamed);
        admin(delete("/api/v1/admin/categories").param("name",renamed)).andExpect(status().isBadRequest());
        admin(delete("/api/v1/admin/categories").param("name",name)).andExpect(status().isNoContent());
        assertThat(items.findById(item.getId())).isPresent();
    }
    @Test void onlyMainAdminCanManageCategories() throws Exception {
        for (String role:List.of("BRANCH_ADMIN","DELIVERY_STAFF")) {
            String username="cat-"+UUID.randomUUID().toString().substring(0,8);
            admin(post("/api/v1/admin/accounts").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("username",username,"role",role,"branchId","11111111-1111-1111-1111-111111111111","password","Category123!","totpSecret","123456"))))
                .andExpect(status().isOk());
            for (var request:List.of(get("/api/v1/admin/categories"),category("Denied"),put("/api/v1/admin/categories").param("name","Breakfast").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Denied\"}"),delete("/api/v1/admin/categories").param("name","Breakfast")))
                mvc.perform(request.with(httpBasic(username,"Category123!")).header("X-Admin-Totp","123456")).andExpect(status().isForbidden());
        }
    }
    @Test void pinsAreMandatoryAndOnlyAnAddressInsideTheBranchRadiusCanOrder() throws Exception {
        Branch branch=new Branch(); branch.setCode("R-"+UUID.randomUUID()); branch.setName("Range test"); branch.setAddress("Test kitchen"); branch.setShopOpen(true);
        branch.setDeliveryRadiusKm(5); branch.setShopLatitude(19.15); branch.setShopLongitude(77.32); branch.setBannerTitle("Test"); branch.setBannerSubtitle("Test"); branches.saveAndFlush(branch);
        String phone="6"+(100000000+new java.security.SecureRandom().nextInt(899999999));
        String token=JsonPathHelper.read(mvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("name","Range customer","phone",phone,"password","TestPass123!"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),"$.token");
        var body=new HashMap<String,Object>(Map.of("branchId",branch.getId(),"customerName","Range customer","customerPhone",phone,"deliveryAddress","Test address","paymentMode","COD","items",List.of(Map.of("menuItemId","8fe9f1a6-3a09-43a0-b1f5-7e426cc271a1","quantity",1))));
        order(body,token).andExpect(status().isBadRequest());
        body.put("deliveryLatitude",19.15); order(body,token).andExpect(status().isBadRequest());
        body.put("deliveryLongitude",77.32); order(body,token).andExpect(status().isOk());
        body.put("deliveryLatitude",20.15); order(body,token).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/features").param("branchId",branch.getId().toString()).param("deliveryLatitude","20.15").param("deliveryLongitude","77.32"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.deliveryAvailable").value(false));
        body.put("deliveryLatitude",19.15); branch.setShopLatitude(null); branch.setShopLongitude(null); branches.saveAndFlush(branch);
        order(body,token).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/features").param("branchId",branch.getId().toString()))
            .andExpect(jsonPath("$.deliveryAvailable").value(false));
        mvc.perform(get("/api/v1/features")).andExpect(jsonPath("$.paymentsEnabled").value(false));
    }
    private ResultActions order(Map<String,Object> body,String token) throws Exception { return mvc.perform(post("/api/v1/orders").header("X-Customer-Token",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))); }
}
