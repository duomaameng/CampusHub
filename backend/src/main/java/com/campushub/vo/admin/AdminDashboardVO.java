package com.campushub.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardVO {

    private Long totalUsers;
    private Long activeTasks;
    private Long inProgressOrders;
    private Long pendingReports;
}
