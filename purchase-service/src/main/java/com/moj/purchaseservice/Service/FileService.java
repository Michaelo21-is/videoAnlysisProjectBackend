package com.moj.purchaseservice.Service;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class FileService {

    private final DataSize maxVideoSize;
    private final Tika tika;
    private final String mp4ContentType;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public FileService(
            @Value("${max-video.size}") DataSize maxVideoSize,
            @Value("${mp4.content-type}") String mp4ContentType,
            Tika tika,
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.maxVideoSize = maxVideoSize;
        this.mp4ContentType = mp4ContentType;
        this.tika = tika;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public String handleVideoFile(MultipartFile videoFile) {
        if (videoFile == null || videoFile.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Video file is required."
            );
        }

        if (videoFile.getSize() > maxVideoSize.toBytes()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The video file must be smaller than 100 MB."
            );
        }

        try {
            String detectedContentType =
                    tika.detect(videoFile.getInputStream());

            if (!mp4ContentType.equals(detectedContentType)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only MP4 video files are supported."
                );
            }

            String objectKey =
                    "videos/" + UUID.randomUUID() + ".mp4";

            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(mp4ContentType)
                            .contentLength(videoFile.getSize())
                            .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(
                            videoFile.getInputStream(),
                            videoFile.getSize()
                    )
            );

            return getPresignedGetUrl(objectKey);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read the uploaded video file.",
                    e
            );
        }
    }

    private String getPresignedGetUrl(String objectKey) {
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(1))
                        .getObjectRequest(getObjectRequest)
                        .build();

        return s3Presigner
                .presignGetObject(presignRequest)
                .url()
                .toExternalForm();
    }
}