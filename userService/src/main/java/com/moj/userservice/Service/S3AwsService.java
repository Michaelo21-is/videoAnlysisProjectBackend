package com.moj.userservice.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3AwsService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${image.max-file-size}")
    private DataSize maxFileSize;

    @Value("${image.allowed-content-types}")
    private List<String> allowedContentTypes;

    public String uploadFile(MultipartFile file) {
        try {

            if (file.getSize() > maxFileSize.toBytes()) {
                throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "File size exceeds the maximum allowed size");
            }
            else if (!allowedContentTypes.contains(file.getContentType())) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File type is not allowed");
            }
            String originalFilename = file.getOriginalFilename();

            String extension =
                    originalFilename != null && originalFilename.contains(".")
                            ? originalFilename.substring(originalFilename.lastIndexOf("."))
                            : "";

            String key =
                    "products/" +
                            UUID.randomUUID() +
                            extension;

            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(file.getBytes())
            );

            return key;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
    public void deleteFile(String key) {
        s3Client.deleteObject(
                b -> b.bucket(bucketName).key(key)
        );
    }
}