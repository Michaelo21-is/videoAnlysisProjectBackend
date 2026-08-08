package com.moj.purchaseservice.Service;

import com.google.genai.Client;
import com.google.genai.types.UploadFileConfig;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileService {

    private final DataSize maxVideoSize;
    private final Tika tika;
    private final String mp4ContentType;
    private final Client geminiClient;

    public FileService(
            @Value("${max-video.size}") DataSize maxVideoSize,
            @Value("${mp4.content-type}") String mp4ContentType,
            Tika tika,
            Client geminiClient
    ) {
        this.maxVideoSize = maxVideoSize;
        this.mp4ContentType = mp4ContentType;
        this.tika = tika;
        this.geminiClient = geminiClient;
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
                    "The video file is too large."
            );
        }

        Path tempFile = null;

        try {
            String detectedContentType =
                    tika.detect(videoFile.getInputStream());

            if (!mp4ContentType.equals(detectedContentType)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only MP4 video files are supported."
                );
            }

            tempFile = Files.createTempFile(
                    "gemini-video-",
                    ".mp4"
            );

            videoFile.transferTo(tempFile);

            com.google.genai.types.File uploadedFile =
                    geminiClient.files.upload(
                            tempFile.toString(),
                            UploadFileConfig.builder()
                                    .mimeType(detectedContentType)
                                    .build()
                    );

            return uploadedFile.uri()
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Gemini did not return a file name"
                            )
                    );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to process the uploaded video.",
                    e
            );

        }
        finally {
            if (tempFile != null){
                try{
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public void deleteFileFromGemini(String videoGeminiLink){
        int fileIndex = videoGeminiLink.indexOf("files/");

        if (fileIndex == -1) {
            throw new IllegalArgumentException(
                    "Invalid Gemini file URI"
            );
        }

        String fileName = videoGeminiLink.substring(fileIndex);

        geminiClient.files.delete(
                fileName,
                null
        );
    }
}