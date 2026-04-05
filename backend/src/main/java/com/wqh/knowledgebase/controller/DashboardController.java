package com.wqh.knowledgebase.controller;

import com.wqh.knowledgebase.common.ApiResponse;
import com.wqh.knowledgebase.dto.DashboardOverview;
import com.wqh.knowledgebase.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverview> overview() {
        return ApiResponse.ok(dashboardService.overview());
    }
}
