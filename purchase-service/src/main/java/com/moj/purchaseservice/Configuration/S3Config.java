package com.moj.purchaseservice.Configuration;

import org.springframework.beans.factory.annotation.Value;import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {
    @Bean
    public Tika tika() {
        return new Tika();
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client() {
        return S3Client.create();
    }
    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
            @Value("${aws.region}") String awsRegion
    ) {
        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}