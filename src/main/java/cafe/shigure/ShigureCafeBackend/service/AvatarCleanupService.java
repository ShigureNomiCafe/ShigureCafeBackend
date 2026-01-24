package cafe.shigure.ShigureCafeBackend.service;

import cafe.shigure.ShigureCafeBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarCleanupService {

    private final UserRepository userRepository;
    private final S3Client s3Client;

    @Value("${application.s3.bucket}")
    private String bucket;

    @Scheduled(cron = "0 0 4 * * ?") // Run at 4 AM every day
    public void cleanupUnusedAvatars() {
        log.info("Starting avatar cleanup job");

        // 1. Fetch all used avatar URLs from the database
        List<String> avatarUrls = userRepository.findAllAvatarUrls();
        Set<String> usedKeys = new HashSet<>();

        // 2. Extract S3 keys (path starting from "avatars/") from URLs
        for (String url : avatarUrls) {
            if (url != null && !url.isEmpty()) {
                int index = url.lastIndexOf("avatars/");
                if (index != -1) {
                    usedKeys.add(url.substring(index));
                }
            }
        }
        log.info("Found {} active avatar keys in database", usedKeys.size());

        // 3. List objects in S3 bucket with prefix "avatars/"
        String prefix = "avatars/";
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse;
        int deletedCount = 0;
        int processedCount = 0;

        do {
            listResponse = s3Client.listObjectsV2(listRequest);

            for (S3Object s3Object : listResponse.contents()) {
                processedCount++;
                String key = s3Object.key();

                // Skip if the file is currently in use
                if (usedKeys.contains(key)) {
                    continue;
                }

                // Skip if the file is new (created within the last 24 hours) to avoid race conditions
                if (s3Object.lastModified().isAfter(Instant.now().minus(24, ChronoUnit.HOURS))) {
                    continue;
                }

                // Delete the unused file
                try {
                    deleteFile(key);
                    deletedCount++;
                    log.debug("Deleted unused avatar: {}", key);
                } catch (Exception e) {
                    log.error("Failed to delete avatar: {}", key, e);
                }
            }

            listRequest = listRequest.toBuilder()
                    .continuationToken(listResponse.nextContinuationToken())
                    .build();

        } while (listResponse.isTruncated());

        log.info("Avatar cleanup job completed. Processed {} files, deleted {}", processedCount, deletedCount);
    }

    private void deleteFile(String key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }
}
