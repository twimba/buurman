package com.buurman.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.buurman.util.SkipTestCoverage;

@Configuration
@EnableAsync
@EnableScheduling
@SkipTestCoverage
public class AsyncConfig {}
