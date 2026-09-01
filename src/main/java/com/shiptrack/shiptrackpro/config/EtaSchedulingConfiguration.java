package com.shiptrack.shiptrackpro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the ETA maintenance job without coupling it to the application class. */
@Configuration
@EnableScheduling
public class EtaSchedulingConfiguration {
}
