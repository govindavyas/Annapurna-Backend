package com.localfood.backend.admin;

import com.localfood.backend.customer.CustomerInsightResponse;
import com.localfood.backend.customer.CustomerInsightsService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerInsightsController {

    private final AdminAuthContext adminAuthContext;
    private final CustomerInsightsService customerInsightsService;

    public AdminCustomerInsightsController(AdminAuthContext adminAuthContext, CustomerInsightsService customerInsightsService) {
        this.adminAuthContext = adminAuthContext;
        this.customerInsightsService = customerInsightsService;
    }

    @GetMapping("/insights")
    public List<CustomerInsightResponse> insights(
        @RequestParam(required = false) UUID branchId,
        @RequestParam(required = false) String query
    ) {
        UUID scopedBranchId = adminAuthContext.requireBranchIdForScopedAdmin();
        return customerInsightsService.listInsights(branchId, scopedBranchId, adminAuthContext.isSuperAdmin(), query);
    }
}
