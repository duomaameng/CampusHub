package com.campushub.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.entity.Application;
import com.campushub.entity.Order;
import com.campushub.entity.Task;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.ApplicationStatus;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.RewardPaymentMethod;
import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import com.campushub.enums.UserRole;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.ApplicationMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CoreFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void shouldLoginSuccessfullyAndReturnJwtToken() throws Exception {
        createUser("student.login@smail.nju.edu.cn", "CampusHub123!", true, UserRole.STUDENT, "LoginUser");

        Map<String, Object> request = new HashMap<>();
        request.put("email", "student.login@smail.nju.edu.cn");
        request.put("password", "CampusHub123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.email").value("student.login@smail.nju.edu.cn"));
    }

    @Test
    void shouldRejectTaskCreationWhenUnauthenticated() throws Exception {
        Map<String, Object> request = buildTaskCreateRequest("Unauthenticated task");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(taskMapper.selectCount(null)).isZero();
    }

    @Test
    void shouldCreateOrderThroughPublishApplyConfirmFlow() throws Exception {
        createUser("publisher@smail.nju.edu.cn", "CampusHub123!", true, UserRole.STUDENT, "Publisher");
        createUser("provider@smail.nju.edu.cn", "CampusHub123!", true, UserRole.STUDENT, "Provider");

        String publisherToken = loginAndGetToken("publisher@smail.nju.edu.cn", "CampusHub123!");
        String providerToken = loginAndGetToken("provider@smail.nju.edu.cn", "CampusHub123!");

        Long taskId = createTaskThroughApi(publisherToken, "Integration flow task");
        Long applicationId = applyTaskThroughApi(providerToken, taskId, "I can take this task today.");
        Long orderId = confirmApplicationThroughApi(publisherToken, applicationId);

        Task task = taskMapper.selectById(taskId);
        Application application = applicationMapper.selectById(applicationId);
        Order order = orderMapper.selectById(orderId);

        assertThat(task).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        assertThat(application).isNotNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);

        assertThat(order).isNotNull();
        assertThat(order.getTaskId()).isEqualTo(taskId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    void shouldRejectProtectedRequestAfterLogoutWithSameToken() throws Exception {
        createUser("student.logout@smail.nju.edu.cn", "CampusHub123!", true, UserRole.STUDENT, "LogoutUser");

        String token = loginAndGetToken("student.logout@smail.nju.edu.cn", "CampusHub123!");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDuplicateApplicationConfirmation() throws Exception {
        createUser("publisher.repeat@smail.nju.edu.cn", "CampusHub123!", true, UserRole.STUDENT, "RepeatPublisher");
        createUser("provider.repeat@smail.nju.edu.cn", "CampusHub123!", true, UserRole.STUDENT, "RepeatProvider");

        String publisherToken = loginAndGetToken("publisher.repeat@smail.nju.edu.cn", "CampusHub123!");
        String providerToken = loginAndGetToken("provider.repeat@smail.nju.edu.cn", "CampusHub123!");

        Long taskId = createTaskThroughApi(publisherToken, "Duplicate confirm task");
        Long applicationId = applyTaskThroughApi(providerToken, taskId, "I want this task.");

        Long firstOrderId = confirmApplicationThroughApi(publisherToken, applicationId);

        mockMvc.perform(post("/api/applications/{applicationId}/confirm", applicationId)
                        .header("Authorization", bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40206));

        assertThat(orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getTaskId, taskId)))
                .isEqualTo(1);

        Order order = orderMapper.selectById(firstOrderId);
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    }

    private User createUser(String email, String rawPassword, boolean verified, UserRole role, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setVerified(verified);
        user.setStudentNoMasked("2418****" + Math.abs(email.hashCode() % 100));
        user.setLoginFailures(0);
        userMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(nickname);
        profile.setCampus("Xianlin");
        profile.setContactVisible(false);
        userProfileMapper.insert(profile);
        return user;
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("token").asText();
    }

    private Long createTaskThroughApi(String token, String title) throws Exception {
        Map<String, Object> request = buildTaskCreateRequest(title);

        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asLong();
    }

    private Long applyTaskThroughApi(String token, Long taskId, String message) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("message", message);

        MvcResult result = mockMvc.perform(post("/api/tasks/{taskId}/applications", taskId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("applicationId").asLong();
    }

    private Long confirmApplicationThroughApi(String token, Long applicationId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications/{applicationId}/confirm", applicationId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("orderId").asLong();
    }

    private Map<String, Object> buildTaskCreateRequest(String title) {
        Map<String, Object> request = new HashMap<>();
        request.put("category", TaskCategory.ERRAND.name());
        request.put("title", title);
        request.put("description", "Integration test task description");
        request.put("campus", "Xianlin");
        request.put("rewardType", RewardType.CASH.name());
        request.put("rewardAmount", 20);
        request.put("paymentMethod", RewardPaymentMethod.CASH.name());
        request.put("deadline", LocalDateTime.now().plusDays(1).toString());
        request.put("anonymous", false);
        request.put("categoryFields", Map.of("pickupLocation", "Dorm 9"));
        return request;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
