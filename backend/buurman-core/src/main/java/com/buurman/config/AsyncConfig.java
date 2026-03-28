package com.buurman.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import com.buurman.util.SkipTestCoverage;

@Configuration
@EnableAsync
@SkipTestCoverage
public class AsyncConfig {}
