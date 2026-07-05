package io.prismguard.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.prismguard.api.model.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent, file-based cache for incremental analysis.
 *
 * <p>On each analysis run the engine can ask whether a source file has changed
 * since the last run by comparing its SHA-256 content hash against the stored
 * value.  Only changed (or previously unseen) files need to be re-analysed, which
 * dramatically reduces analysis time on large projects.
 *
 * <p>The cache is serialised as a JSON file ({@value #CACHE_FILE}) in the project
 * root directory.  The file is updated atomically by writing to a temporary file
 * and then renaming it over the target.
 *
 * <p><b>Not yet wired</b>: this class is intentionally <em>not</em> annotated with
 * {@code @Component}. To activate incremental analysis, inject this bean into
 * {@link io.prismguard.core.scanner.JavaProjectScanner} and call
 * {@link #needsAnalysis}/{@link #save} around each file scan.
 */
public class IncrementalAnalysisCache {

    private static final Logger log = LoggerFactory.getLogger(IncrementalAnalysisCache.class);

    /** Name of the on-disk cache file, written to the project root. */
    private static final String CACHE_FILE = ".prismguard-cache.json";

    private final ObjectMapper objectMapper;

    public IncrementalAnalysisCache() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Loads the cache from the project root directory.
     *
     * <p>If the cache file does not exist (first run) or cannot be read / parsed,
     * an empty map is returned and the error is logged at {@code WARN} level so
     * that the caller falls back to a full analysis without failing.
     *
     * @param projectRoot the root directory of the project
     * @return mutable map of relative file path → {@link CachedFileResult};
     *         may be empty, never {@code null}
     */
    public Map<String, CachedFileResult> load(Path projectRoot) {
        Path cacheFile = projectRoot.resolve(CACHE_FILE);
        if (!Files.exists(cacheFile)) {
            log.debug("No incremental cache found at {}, starting fresh", cacheFile);
            return new HashMap<>();
        }

        try (InputStream in = Files.newInputStream(cacheFile)) {
            Map<String, CachedFileResult> cache = objectMapper.readValue(
                    in, new TypeReference<Map<String, CachedFileResult>>() {});
            log.debug("Loaded incremental cache: {} entries from {}", cache.size(), cacheFile);
            return new HashMap<>(cache);
        } catch (IOException e) {
            log.warn("Failed to read incremental cache at {}; starting fresh. Cause: {}",
                    cacheFile, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Persists the cache to the project root directory.
     *
     * <p>The write is performed via a temp-file rename to avoid leaving a corrupt
     * cache file if the JVM is interrupted mid-write.  Any I/O errors are logged
     * at {@code WARN} level; the caller is not expected to handle them.
     *
     * @param projectRoot the root directory of the project
     * @param cache       the cache data to persist
     */
    public void save(Path projectRoot, Map<String, CachedFileResult> cache) {
        Path cacheFile = projectRoot.resolve(CACHE_FILE);
        Path tempFile = projectRoot.resolve(CACHE_FILE + ".tmp");

        try {
            Files.createDirectories(projectRoot);
            try (OutputStream out = Files.newOutputStream(tempFile,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, cache);
            }
            Files.move(tempFile, cacheFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            log.debug("Saved incremental cache: {} entries to {}", cache.size(), cacheFile);
        } catch (IOException e) {
            log.warn("Failed to save incremental cache to {}. Cause: {}", cacheFile, e.getMessage());
            // Best-effort – silently clean up temp file
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // nothing we can do
            }
        }
    }

    /**
     * Computes the SHA-256 hash of the file at the given path and returns it as a
     * lowercase hex string.
     *
     * @param file path to the file to hash
     * @return 64-character lowercase hex SHA-256 digest
     * @throws IOException if the file cannot be read
     */
    public String computeFileHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream in = Files.newInputStream(file)) {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java SE specification; this cannot happen
            throw new IllegalStateException("SHA-256 MessageDigest not available", e);
        }
    }

    /**
     * Returns {@code true} if the file requires (re-)analysis.
     *
     * <p>A file needs analysis if:
     * <ul>
     *   <li>it has never been analysed (no cache entry), or</li>
     *   <li>its current SHA-256 hash differs from the cached hash, or</li>
     *   <li>the hash cannot be computed (I/O error).</li>
     * </ul>
     *
     * @param file  path to the source file
     * @param cache the current cache, keyed by absolute file path string
     * @return {@code true} if the file should be analysed; {@code false} if the
     *         cached result can be reused
     */
    public boolean needsAnalysis(Path file, Map<String, CachedFileResult> cache) {
        String key = file.toAbsolutePath().toString();
        CachedFileResult cached = cache.get(key);
        if (cached == null) {
            return true;
        }

        try {
            String currentHash = computeFileHash(file);
            boolean changed = !currentHash.equals(cached.fileHash());
            if (changed) {
                log.debug("File {} has changed (hash mismatch), will re-analyse", file);
            }
            return changed;
        } catch (IOException e) {
            log.warn("Cannot compute hash for {}, treating as changed. Cause: {}", file, e.getMessage());
            return true;
        }
    }

    // -----------------------------------------------------------------------
    // Cache record
    // -----------------------------------------------------------------------

    /**
     * Immutable snapshot of the analysis results for a single source file.
     *
     * @param fileHash   SHA-256 hex digest of the file content at analysis time
     * @param analyzedAt the instant at which the file was last analysed
     * @param findings   the findings produced by all rules for this file
     */
    public record CachedFileResult(
            String fileHash,
            Instant analyzedAt,
            List<Finding> findings
    ) {
        public CachedFileResult {
            if (fileHash == null || fileHash.isBlank()) {
                throw new IllegalArgumentException("fileHash must not be null or blank");
            }
            if (analyzedAt == null) {
                throw new IllegalArgumentException("analyzedAt must not be null");
            }
            findings = findings != null ? List.copyOf(findings) : List.of();
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
