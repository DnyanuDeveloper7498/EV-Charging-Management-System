package com.evcharging.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
@EnableAsync
public class AppConfig {

    /**
     * All business "now" comparisons use this clock. The browser sends booking times as local date-times
     * without a zone, so the server must compare them in the users' zone (default Asia/Kolkata) even when it
     * is deployed on a UTC host such as Render or AWS.
     */
    @Bean
    public Clock clock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
