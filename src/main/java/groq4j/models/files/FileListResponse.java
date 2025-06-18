package groq4j.models.files;

import groq4j.enums.FilePurpose;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Response object for listing files from the Groq API.
 * 
 * <p>This record contains the list of files and metadata about the response,
 * along with utility methods for filtering and analyzing the files.
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Get file list response
 * FileListResponse response = filesService.listFiles();
 * 
 * // Basic information
 * int totalFiles = response.getTotalCount();
 * long totalSize = response.getTotalSize();
 * 
 * // Filter files
 * List<FileObject> batchFiles = response.getBatchFiles();
 * List<FileObject> largeFiles = response.getLargeFiles();
 * List<FileObject> recentFiles = response.getRecentFiles(7); // last 7 days
 * 
 * // Find specific files
 * Optional<FileObject> file = response.findByFilename("data.jsonl");
 * Optional<FileObject> file = response.findById("file-abc123");
 * }</pre>
 */
public record FileListResponse(
    /**
     * The object type, which is always "list".
     */
    String object,
    
    /**
     * List of file objects.
     */
    List<FileObject> data
) {
    
    /**
     * Creates a new FileListResponse.
     *
     * @param object the object type (should be "list")
     * @param data the list of files
     * @return a new FileListResponse instance
     */
    public static FileListResponse of(String object, List<FileObject> data) {
        return new FileListResponse(object, data);
    }
    
    /**
     * Creates a FileListResponse with just the file list (object will be "list").
     *
     * @param data the list of files
     * @return a new FileListResponse instance
     */
    public static FileListResponse of(List<FileObject> data) {
        return new FileListResponse("list", data);
    }
    
    /**
     * Returns the total number of files.
     *
     * @return the number of files in the response
     */
    public int getTotalCount() {
        return data != null ? data.size() : 0;
    }
    
    /**
     * Returns the total size of all files in bytes.
     *
     * @return the sum of all file sizes in bytes
     */
    public long getTotalSize() {
        if (data == null) {
            return 0;
        }
        return data.stream()
            .mapToLong(FileObject::bytes)
            .sum();
    }
    
    /**
     * Returns a human-readable representation of the total size.
     *
     * @return formatted total size (e.g., "15.2 MB")
     */
    public String getDisplayTotalSize() {
        long totalBytes = getTotalSize();
        if (totalBytes < 1024) {
            return totalBytes + " B";
        } else if (totalBytes < 1024 * 1024) {
            return String.format("%.1f KB", totalBytes / 1024.0);
        } else if (totalBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", totalBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Checks if the file list is empty.
     *
     * @return true if there are no files
     */
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }
    
    /**
     * Returns files filtered by purpose.
     *
     * @param purpose the file purpose to filter by
     * @return list of files with the specified purpose
     */
    public List<FileObject> getFilesByPurpose(FilePurpose purpose) {
        if (data == null) {
            return List.of();
        }
        return data.stream()
            .filter(file -> file.purpose() == purpose)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns all batch processing files.
     *
     * @return list of files with BATCH purpose
     */
    public List<FileObject> getBatchFiles() {
        return getFilesByPurpose(FilePurpose.BATCH);
    }
    
    /**
     * Returns files larger than the specified size.
     *
     * @param sizeInBytes the minimum file size in bytes
     * @return list of files larger than the specified size
     */
    public List<FileObject> getFilesLargerThan(long sizeInBytes) {
        if (data == null) {
            return List.of();
        }
        return data.stream()
            .filter(file -> file.bytes() > sizeInBytes)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns files considered large (> 10 MB).
     *
     * @return list of large files
     */
    public List<FileObject> getLargeFiles() {
        return getFilesLargerThan(10 * 1024 * 1024); // 10 MB
    }
    
    /**
     * Returns files created within the specified number of days.
     *
     * @param days the number of days to look back
     * @return list of files created within the specified timeframe
     */
    public List<FileObject> getRecentFiles(int days) {
        if (data == null) {
            return List.of();
        }
        long cutoffTime = System.currentTimeMillis() / 1000 - (days * 24 * 60 * 60);
        return data.stream()
            .filter(file -> file.createdAt() > cutoffTime)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns JSONL files (based on file extension).
     *
     * @return list of files with .jsonl extension
     */
    public List<FileObject> getJsonlFiles() {
        if (data == null) {
            return List.of();
        }
        return data.stream()
            .filter(FileObject::isJsonlFile)
            .collect(Collectors.toList());
    }
    
    /**
     * Finds a file by its unique identifier.
     *
     * @param fileId the file ID to search for
     * @return the file if found, empty otherwise
     */
    public Optional<FileObject> findById(String fileId) {
        if (data == null || fileId == null) {
            return Optional.empty();
        }
        return data.stream()
            .filter(file -> fileId.equals(file.id()))
            .findFirst();
    }
    
    /**
     * Finds files by filename (exact match).
     *
     * @param filename the filename to search for
     * @return list of files with the specified filename
     */
    public List<FileObject> findByFilename(String filename) {
        if (data == null || filename == null) {
            return List.of();
        }
        return data.stream()
            .filter(file -> filename.equals(file.filename()))
            .collect(Collectors.toList());
    }
    
    /**
     * Finds the first file with the specified filename.
     *
     * @param filename the filename to search for
     * @return the first file with the specified filename, empty if not found
     */
    public Optional<FileObject> findFirstByFilename(String filename) {
        List<FileObject> files = findByFilename(filename);
        return files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
    }
    
    /**
     * Returns files sorted by creation time (newest first).
     *
     * @return list of files sorted by creation time in descending order
     */
    public List<FileObject> getSortedByNewest() {
        if (data == null) {
            return List.of();
        }
        return data.stream()
            .sorted((a, b) -> Long.compare(b.createdAt(), a.createdAt()))
            .collect(Collectors.toList());
    }
    
    /**
     * Returns files sorted by size (largest first).
     *
     * @return list of files sorted by size in descending order
     */
    public List<FileObject> getSortedBySize() {
        if (data == null) {
            return List.of();
        }
        return data.stream()
            .sorted((a, b) -> Long.compare(b.bytes(), a.bytes()))
            .collect(Collectors.toList());
    }
    
    /**
     * Returns a summary of the file collection.
     *
     * @return a string summarizing the files (count, total size, purposes)
     */
    public String getSummary() {
        if (isEmpty()) {
            return "No files";
        }
        
        int totalCount = getTotalCount();
        String totalSize = getDisplayTotalSize();
        int batchCount = getBatchFiles().size();
        int largeCount = getLargeFiles().size();
        
        return String.format("%d files (%s total) - %d batch, %d large", 
                           totalCount, totalSize, batchCount, largeCount);
    }
}