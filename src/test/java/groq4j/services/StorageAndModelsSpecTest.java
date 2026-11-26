package groq4j.services;

import groq4j.builders.BatchRequestBuilder;
import groq4j.builders.FileUploadRequestBuilder;
import groq4j.enums.FilePurpose;
import groq4j.models.batch.BatchListResponse;
import groq4j.models.batch.BatchRequest;
import groq4j.models.batch.BatchResponse;
import groq4j.enums.BatchStatus;
import groq4j.models.files.FileDeleteResponse;
import groq4j.models.files.FileListResponse;
import groq4j.models.files.FileObject;
import groq4j.models.models.Model;
import groq4j.models.models.ModelListResponse;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Models, files and batches against a fake Groq server")
class StorageAndModelsSpecTest extends FakeServerTestBase {

    private ModelsService models() {
        return new ModelsServiceImpl(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    private FilesService files() {
        return new FilesServiceImpl(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    private BatchService batches() {
        return new BatchServiceImpl(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    @Test
    void listsModels() {
        ModelListResponse response = models().listModels();

        assertEquals(2, response.getModelCount());
        assertEquals("openai/gpt-oss-20b", response.data().getFirst().id());
        assertEquals("/openai/v1/models", server.requestAt("/openai/v1/models").path());
        assertEquals("GET", server.requestAt("/openai/v1/models").method());
        assertEquals("Bearer test-key", server.requestAt("/openai/v1/models").header("Authorization"));
    }

    @Test
    void retrievesOneModel() {
        Model model = models().retrieveModel("llama-3.3-70b-versatile");

        assertEquals("llama-3.3-70b-versatile", model.id());
        assertEquals(131072, model.contextWindow());
        assertEquals("/openai/v1/models/llama-3.3-70b-versatile",
            server.requestAt("/openai/v1/models/llama-3.3-70b-versatile").path());
    }

    @Test
    void uploadsFileAsMultipart() {
        byte[] jsonl = "{\"custom_id\":\"1\"}\n".getBytes(StandardCharsets.UTF_8);

        FileObject file = files().uploadFile(jsonl, "requests.jsonl", FilePurpose.BATCH);

        assertEquals("file-1", file.id());
        assertEquals(1024, file.bytes());
        var request = server.requestTo("/files");
        assertTrue(request.header("Content-Type").startsWith("multipart/form-data"));
        assertTrue(request.body().contains("filename=\"requests.jsonl\""));
        assertTrue(request.body().contains("name=\"purpose\""));
        assertTrue(request.body().contains("batch"));
    }

    @Test
    void uploadsThroughTheBuilderAndDefaultHelper() {
        byte[] jsonl = "{\"custom_id\":\"1\"}\n".getBytes(StandardCharsets.UTF_8);

        FileObject built = files().uploadFile(FileUploadRequestBuilder.createBatchFile(jsonl, "batch.jsonl").build());
        FileObject helper = files().uploadBatchFile(jsonl, "batch2.jsonl");

        assertEquals("file-1", built.id());
        assertEquals("file-1", helper.id());
        assertEquals(2, server.requests().size());
    }

    @Test
    void listsFilesAndReadsBothPurposes() {
        FileListResponse list = files().listFiles();

        assertEquals(2, list.getTotalCount());
        assertTrue(list.getFilesByPurpose(FilePurpose.BATCH_OUTPUT).size() == 1);
        assertEquals("requests.jsonl", list.getBatchFiles().getFirst().filename());
        assertEquals("output.jsonl", list.getFilesByPurpose(FilePurpose.BATCH_OUTPUT).getFirst().filename());
    }

    @Test
    void retrievesDeletesAndDownloads() {
        FileObject file = files().retrieveFile("file-1");
        FileDeleteResponse deleted = files().deleteFile("file-1");
        byte[] content = files().downloadFileContent("file-1");

        assertEquals("file-1", file.id());
        assertTrue(deleted.deleted());
        assertEquals("{\"id\":\"batch_req_1\",\"custom_id\":\"1\",\"response\":{\"status_code\":200}}",
            new String(content, StandardCharsets.UTF_8));
        assertEquals("DELETE", server.requestTo("/files/file-1").method());
        assertEquals("/openai/v1/files/file-1/content", server.requestTo("/content").path());
    }

    @Test
    void convenienceHelpersReportExistingFiles() {
        assertTrue(files().fileExists("file-1"));
        assertEquals(1024, files().getFileSize("file-1"));
    }

    @Test
    void convenienceHelpersSwallowFailures() {
        server.route("/files/missing", request -> FakeGroqServer.Reply.error(
            404, "no such file", "not_found_error", "not_found"));

        assertFalse(files().fileExists("missing"));
        assertEquals(-1, files().getFileSize("missing"));
    }

    @Test
    void createsBatchWithMetadata() {
        BatchRequest request = BatchRequestBuilder.create("file-1")
            .completionWindow("24h")
            .metadata("project", "tests")
            .build();

        BatchResponse batch = batches().createBatch(request);

        assertEquals("batch_1", batch.id());
        assertEquals(BatchStatus.IN_PROGRESS, batch.status());
        assertEquals("file-1", batch.inputFileId());
        assertEquals(2, batch.requestCounts().total());
        assertTrue(server.requestTo("/batches").body().contains("\"metadata\":{\"project\":\"tests\"}"));
        assertTrue(server.requestTo("/batches").body().contains("\"completion_window\":\"24h\""));
    }

    @Test
    void listsRetrievesAndCancelsBatches() {
        BatchListResponse list = batches().listBatches();
        BatchResponse retrieved = batches().retrieveBatch("batch_1");
        BatchResponse cancelled = batches().cancelBatch("batch_1");

        assertEquals(1, list.getBatchCount());
        assertTrue(list.getCompletedBatches().size() == 1);
        assertEquals("batch_1", retrieved.id());
        assertEquals(BatchStatus.CANCELLED, cancelled.status());
        assertEquals("POST", server.requestTo("/cancel").method());
    }
}
