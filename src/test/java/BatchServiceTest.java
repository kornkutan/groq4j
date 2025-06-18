import groq4j.builders.BatchRequestBuilder;
import groq4j.enums.BatchStatus;
import groq4j.models.batch.BatchRequest;
import groq4j.models.batch.BatchResponse;
import groq4j.models.batch.BatchListResponse;
import groq4j.services.BatchService;
import groq4j.services.BatchServiceImpl;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Batch Service Tests")
@Tag("integration")
@Tag("requires-api-key")
@Tag("premium-feature")
class BatchServiceTest extends BaseServiceTest {
    
    private BatchService batchService;
    
    @BeforeAll
    void setup() {
        super.baseSetup();
        requireServiceEnabled("Batch", isBatchEnabled());
        batchService = BatchServiceImpl.create(getApiKey());
        logTestProgress("BatchService initialized for testing (premium features expected to be limited)");
    }
        
    @Test
    @DisplayName("List all batches should return valid response or expected premium error")
    void testListAllBatches() {
        requireApiKey();
        
        try {
            BatchListResponse response = batchService.listBatches();
            
            // If successful, validate response
            assertNotNull(response, "Response should not be null");
            assertNotNull(response.object(), "Response object should not be null");
            assertTrue(response.getBatchCount() >= 0, "Batch count should be non-negative");
            
            logTestProgress("List all batches test passed - found " + response.getBatchCount() + " batches");
        } catch (Exception e) {
            // Expected for non-premium accounts
            if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("premium") || e.getMessage().contains("Not available for your plan"))) {
                logTestProgress("List batches requires premium access: " + e.getMessage());
                Assumptions.assumeTrue(false, "Batch operations require premium access");
            } else {
                throw e; // Re-throw unexpected errors
            }
        }
    }
        
    @Test
    @DisplayName("Filter batches by status should work correctly if premium access available")
    void testFilterBatchesByStatus() {
        requireApiKey();
        
        try {
            BatchListResponse response = batchService.listBatches();
            
            // Test all status filtering methods
            List<BatchResponse> inProgress = response.getInProgressBatches();
            List<BatchResponse> completed = response.getCompletedBatches();
            List<BatchResponse> failed = response.getFailedBatches();
            List<BatchResponse> cancelled = response.getCancelledBatches();
            List<BatchResponse> expired = response.getExpiredBatches();
            List<BatchResponse> terminal = response.getTerminalBatches();
            List<BatchResponse> cancellable = response.getCancellableBatches();
            
            // Validate filter methods work
            assertNotNull(inProgress, "In progress batches list should not be null");
            assertNotNull(completed, "Completed batches list should not be null");
            assertNotNull(failed, "Failed batches list should not be null");
            assertNotNull(cancelled, "Cancelled batches list should not be null");
            assertNotNull(expired, "Expired batches list should not be null");
            assertNotNull(terminal, "Terminal batches list should not be null");
            assertNotNull(cancellable, "Cancellable batches list should not be null");
            
            // Test status statistics
            Map<BatchStatus, Long> stats = response.getStatusStatistics();
            assertNotNull(stats, "Status statistics should not be null");
            
            logTestProgress("Batch status filtering test passed - all filter methods work");
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("premium") || e.getMessage().contains("Not available for your plan"))) {
                logTestProgress("Batch filtering requires premium access: " + e.getMessage());
                Assumptions.assumeTrue(false, "Batch operations require premium access");
            } else {
                throw e;
            }
        }
    }
        
    @Test
    @DisplayName("Batch utilities and analysis should work correctly")
    void testBatchUtilitiesAndAnalysis() {
        requireApiKey();
        
        try {
            BatchListResponse response = batchService.listBatches();
            
            // Test utility methods work regardless of data
            assertTrue(response.getBatchCount() >= 0, "Batch count should be non-negative");
            assertNotNull(response.hasMore(), "Has more should not be null");
            assertTrue(response.getOverallCompletionRate() >= 0, "Completion rate should be non-negative");
            
            List<String> batchIds = response.getBatchIds();
            assertNotNull(batchIds, "Batch IDs list should not be null");
            assertEquals(response.getBatchCount(), batchIds.size(), "Batch IDs count should match total count");
            
            if (!response.isEmpty()) {
                // Test utility methods with data
                assertDoesNotThrow(() -> response.getMostRecentBatch(), "Should be able to get most recent batch");
                assertDoesNotThrow(() -> response.getOldestBatch(), "Should be able to get oldest batch");
                
                List<BatchResponse> withOutput = response.getBatchesWithOutput();
                List<BatchResponse> withErrors = response.getBatchesWithErrors();
                assertNotNull(withOutput, "Batches with output should not be null");
                assertNotNull(withErrors, "Batches with errors should not be null");
            }
            
            logTestProgress("Batch utilities test passed - all utility methods work correctly");
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("premium") || e.getMessage().contains("Not available for your plan"))) {
                logTestProgress("Batch utilities require premium access: " + e.getMessage());
                Assumptions.assumeTrue(false, "Batch operations require premium access");
            } else {
                throw e;
            }
        }
    }
        
    @Test
    @DisplayName("Retrieve specific batch should work if batches exist")
    void testRetrieveSpecificBatch() {
        requireApiKey();
        
        try {
            BatchListResponse listResponse = batchService.listBatches();
            
            if (!listResponse.isEmpty()) {
                String batchId = listResponse.data().get(0).id();
                
                BatchResponse batch = batchService.retrieveBatch(batchId);
                
                assertNotNull(batch, "Retrieved batch should not be null");
                assertEquals(batchId, batch.id(), "Retrieved batch ID should match request");
                assertNotNull(batch.object(), "Batch object should not be null");
                assertNotNull(batch.endpoint(), "Batch endpoint should not be null");
                
                logTestProgress("Retrieve specific batch test passed - " + batch.id());
            } else {
                logTestProgress("No batches available to test retrieval (expected for new accounts)");
            }
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("premium") || e.getMessage().contains("Not available for your plan"))) {
                logTestProgress("Batch retrieval requires premium access: " + e.getMessage());
                Assumptions.assumeTrue(false, "Batch operations require premium access");
            } else {
                throw e;
            }
        }
    }
        
    @Test
    @DisplayName("Invalid batch retrieval should throw appropriate exception")
    void testErrorHandlingInvalidBatch() {
        requireApiKey();
        
        // This test should throw an exception regardless of premium status
        assertThrows(Exception.class, () -> {
            batchService.retrieveBatch("batch_definitely_non_existent_12345");
        }, "Invalid batch ID should throw an exception");
        
        logTestProgress("Error handling test passed");
    }
        
    @Test
    @DisplayName("BatchRequestBuilder validation and creation should work correctly")
    void testBatchRequestBuilderValidation() {
        // This test doesn't require API access, just builder validation
        
        // Test basic builder creation
        BatchRequest simpleRequest = BatchRequestBuilder.create(TestConstants.TEST_FILE_NAME)
            .completionWindow("24h")
            .build();
        
        assertNotNull(simpleRequest, "Simple request should not be null");
        assertEquals(TestConstants.TEST_FILE_NAME, simpleRequest.inputFileId(), "Input file ID should match");
        assertNotNull(simpleRequest.endpoint(), "Endpoint should not be null");
        assertEquals("24h", simpleRequest.completionWindow(), "Completion window should match");
        assertFalse(simpleRequest.hasMetadata(), "Simple request should not have metadata");
        
        // Test builder with metadata
        BatchRequest metadataRequest = BatchRequestBuilder.create("file-test-456")
            .completionWindow("48h")
            .metadata("project", "test-batch")
            .metadata("priority", "high")
            .description("Test batch with metadata")
            .build();
        
        assertTrue(metadataRequest.hasMetadata(), "Metadata request should have metadata");
        assertEquals(3, metadataRequest.getMetadataOrEmpty().size(), "Should have 3 metadata items");
        assertEquals("test-batch", metadataRequest.getMetadataOrEmpty().get("project"), "Project metadata should match");
        
        // Test convenience methods
        BatchRequest convenienceRequest = BatchRequestBuilder.create("file-test-789")
            .in7Days()
            .priority("low")
            .project("experimental")
            .build();
        
        assertEquals("7d", convenienceRequest.completionWindow(), "7 days window should be set");
        assertEquals(7, convenienceRequest.getCompletionWindowValue(), "Window value should be 7");
        assertEquals("d", convenienceRequest.getCompletionWindowUnit(), "Window unit should be 'd'");
        
        // Test validation - empty input file ID
        assertThrows(Exception.class, () -> {
            BatchRequestBuilder.create("")
                .completionWindow("24h")
                .build();
        }, "Empty input file ID should throw exception");
        
        // Test validation - invalid completion window
        assertThrows(Exception.class, () -> {
            BatchRequestBuilder.create("file-test")
                .completionWindow("invalid")
                .build();
        }, "Invalid completion window should throw exception");
        
        logTestProgress("BatchRequestBuilder validation test passed");
    }
        
    @Test
    @DisplayName("Batch model validation and utilities should work correctly")
    void testBatchModelUtilities() {
        // Test batch request utilities (no API access needed)
        BatchRequest request1 = BatchRequest.simple("file-123", "24h");
        BatchRequest request2 = BatchRequest.withMetadata("file-456", "48h", Map.of("project", "test"));
        
        assertTrue(request1.isValidCompletionWindow(), "24h should be valid completion window");
        assertEquals(24, request1.getCompletionWindowValue(), "Window value should be 24");
        assertEquals("h", request1.getCompletionWindowUnit(), "Window unit should be 'h'");
        assertFalse(request1.hasMetadata(), "Simple request should not have metadata");
        
        assertTrue(request2.hasMetadata(), "Metadata request should have metadata");
        assertEquals(1, request2.getMetadataOrEmpty().size(), "Should have 1 metadata item");
        assertEquals("test", request2.getMetadataOrEmpty().get("project"), "Project metadata should match");
        
        // Test batch response utilities if available (requires API access)
        assertDoesNotThrow(() -> {
            try {
                BatchListResponse response = batchService.listBatches();
                if (!response.isEmpty()) {
                    BatchResponse sampleBatch = response.data().get(0);
                    
                    // Test utility methods exist and work
                    assertNotNull(sampleBatch.isTerminal(), "isTerminal should not be null");
                    assertNotNull(sampleBatch.isCancellable(), "isCancellable should not be null");
                    assertNotNull(sampleBatch.getFormattedStatus(), "Formatted status should not be null");
                    assertTrue(sampleBatch.getDurationSinceCreated() >= 0, "Duration should be non-negative");
                    assertTrue(sampleBatch.getCompletionPercentage() >= 0, "Completion percentage should be non-negative");
                }
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("premium") || e.getMessage().contains("Not available for your plan"))) {
                    // Expected for non-premium accounts
                    logTestProgress("Batch response utilities require premium access");
                } else {
                    throw e;
                }
            }
        });
        
        logTestProgress("Batch model utilities test passed");
    }
        
    @Nested
    @DisplayName("Performance and API Endpoint Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("Performance and API endpoints should work efficiently")
        void testPerformanceAndApiEndpoints() {
            requireApiKey();
            
            try {
                long startTime = System.currentTimeMillis();
                BatchListResponse response = batchService.listBatches();
                long endTime = System.currentTimeMillis();
                
                long duration = endTime - startTime;
                assertTrue(duration < 10000, "API call should complete within 10 seconds");
                
                assertTrue(response.getBatchCount() >= 0, "Batch count should be non-negative");
                assertNotNull(response.hasMore(), "Has more should not be null");
                
                // Test batch existence methods if batches available
                if (!response.isEmpty()) {
                    String existingBatchId = response.data().get(0).id();
                    
                    assertTrue(response.hasBatch(existingBatchId), "Should find existing batch");
                    
                    BatchResponse foundBatch = response.findBatch(existingBatchId);
                    assertEquals(existingBatchId, foundBatch.id(), "Found batch ID should match");
                }
                
                logTestProgress("Performance test passed - API call took " + duration + "ms");
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("premium") || e.getMessage().contains("Not available for your plan"))) {
                    logTestProgress("Performance testing requires premium access: " + e.getMessage());
                    Assumptions.assumeTrue(false, "Batch operations require premium access");
                } else {
                    throw e;
                }
            }
        }
    }
        
    // Note: Batch creation and cancellation tests are not included because:
    // 1. Creating batches requires uploading files first (FilesService dependency)
    // 2. Cancelling batches might interfere with real user batches
    // 3. These operations cost money and create side effects
    // 4. Most accounts don't have premium batch access
    
    private static String formatTimestamp(long timestamp) {
        return java.time.Instant.ofEpochSecond(timestamp).toString();
    }
    
    private static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}