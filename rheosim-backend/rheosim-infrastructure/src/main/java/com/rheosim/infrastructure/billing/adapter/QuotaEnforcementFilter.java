package com.rheosim.infrastructure.billing.adapter;

import com.rheosim.application.billing.usecase.BillingUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class QuotaEnforcementFilter extends OncePerRequestFilter {

    private final BillingUseCase billingUseCase;

    private static final Map<String, String> FEATURE_ROUTES = Map.of(
            "/api/v1/simulation/fem3d", "fem_3d",
            "/api/v1/simulation/gpu", "gpu_compute",
            "/api/v1/ml/predict", "ml_calibration",
            "/api/v1/marketplace/plugins/upload", "marketplace_upload"
    );

    public QuotaEnforcementFilter(BillingUseCase billingUseCase) {
        this.billingUseCase = billingUseCase;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        String requiredFeature = FEATURE_ROUTES.entrySet().stream()
                .filter(e -> path.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (requiredFeature != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                try {
                    UUID userId = UUID.fromString(auth.getName());
                    billingUseCase.enforceQuota(userId, requiredFeature);
                } catch (IllegalStateException e) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
                    return;
                } catch (IllegalArgumentException ignored) {
                    // Non-UUID principal, skip enforcement
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
