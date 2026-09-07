package com.shiptrack.shiptrackpro.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Removes the old one-route-per-shipment constraint from existing local databases. */
@Configuration
@RequiredArgsConstructor
public class RouteSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner removeLegacyRouteConstraint() {
        return args -> jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS routes DROP CONSTRAINT IF EXISTS uk_routes_shipment_id"
        );
    }
}
