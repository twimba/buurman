package com.buurman.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

@Service
@Slf4j
public class ThumbnailService {

    private static final int MAX_DIMENSION = 400;
    private static final double JPEG_QUALITY = 0.8;

    public Optional<byte[]> generateThumbnail(InputStream inputStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(inputStream)
                    .size(MAX_DIMENSION, MAX_DIMENSION)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toOutputStream(baos);
            return Optional.of(baos.toByteArray());
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
