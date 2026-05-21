package com.portfolio.task_management_system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.portfolio.task_management_system.config.SpringSecurity;
import com.portfolio.task_management_system.filter.JwtFilter;
import com.portfolio.task_management_system.filter.RateLimitingFilter;
import com.portfolio.task_management_system.service.TaskService;

@WebMvcTest(
        controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        SpringSecurity.class,
                        JwtFilter.class,
                        RateLimitingFilter.class
                }))
@Import(AdminControllerSecurityTest.SecurityTestConfig.class)
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void anonymousRequestToAdminEndpointReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/tasks/deleted"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBearerTokenRequestToAdminEndpointReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/tasks/deleted")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRoleRequestToAdminEndpointReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/tasks/deleted")
                .with(user("shivam").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleRequestToAdminEndpointSucceeds() throws Exception {
        when(taskService.getDeletedTasks(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/tasks/deleted")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        @Order(1)
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(request -> request
                            .requestMatchers(HttpMethod.GET, "/api/admin/tasks/deleted").hasRole("ADMIN")
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated())
                    .build();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("users", "tasks");
        }
    }
}
