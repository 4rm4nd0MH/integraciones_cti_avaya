package com.pruebafullstack.cti;

import com.pruebafullstack.cti.config.AppCorsProperties;
import com.pruebafullstack.cti.config.CtiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({CtiProperties.class, AppCorsProperties.class})
public class CtiDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(CtiDashboardApplication.class, args);
    }
}
