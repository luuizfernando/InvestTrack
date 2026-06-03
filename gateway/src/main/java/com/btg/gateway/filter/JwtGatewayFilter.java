package com.btg.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (JwtAuthenticationToken) ctx.getAuthentication())
                .flatMap(auth -> {
                    Jwt jwt = auth.getToken();
                    String userId = jwt.getSubject();
                    String role = extractRole(jwt);
                    String requestId = Optional
                            .ofNullable(exchange.getRequest().getHeaders().getFirst("X-Request-Id"))
                            .orElse(UUID.randomUUID().toString());

                    ServerWebExchange mutated = exchange.mutate()
                            .request(r -> r
                                    .header("X-User-Id", userId)
                                    .header("X-User-Role", role)
                                    .header("X-Request-Id", requestId))
                            .build();

                    return chain.filter(mutated);
                });
    }

    private String extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object roles = realmAccess.get("roles");
            if (roles instanceof List<?> list && !list.isEmpty()) {
                return list.getFirst().toString();
            }
        }

        List<String> roles = jwt.getClaim("roles");
        if (roles != null && !roles.isEmpty()) {
            return roles.getFirst();
        }

        return "";
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
