package com.banquito.switchpagos.sftp.client;

import com.banquito.switchpagos.sftp.config.BatchServiceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

@Component
public class BatchServiceClient {

    private final BatchServiceProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BatchServiceClient(BatchServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public BatchUploadResult upload(
            Path file,
            String companyRuc,
            String companyCustomerUuid,
            String username) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("companyRuc", companyRuc);
        if (StringUtils.hasText(companyCustomerUuid)) {
            body.add("companyCustomerUuid", companyCustomerUuid);
        }
        body.add("channel", "SFTP");
        body.add("receivedBy", username);

        try {
            String response = restClient.post()
                    .uri(properties.getUploadPath())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode json = response == null || response.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response);
            String batchId = text(json, "batchId");
            String status = text(json, "status");
            return BatchUploadResult.accepted(batchId, status, 202, response);
        } catch (HttpStatusCodeException exception) {
            return BatchUploadResult.rejected(
                    exception.getStatusCode().value(),
                    errorCode(exception),
                    errorMessage(exception),
                    exception.getResponseBodyAsString());
        } catch (RestClientResponseException exception) {
            return BatchUploadResult.rejected(
                    exception.getStatusCode().value(),
                    errorCode(exception),
                    errorMessage(exception),
                    exception.getResponseBodyAsString());
        } catch (RestClientException exception) {
            return BatchUploadResult.rejected(0, "BATCH_SERVICE_UNAVAILABLE", safe(exception.getMessage()), null);
        } catch (Exception exception) {
            return BatchUploadResult.rejected(0, "BATCH_SERVICE_RESPONSE_ERROR", safe(exception.getMessage()), null);
        }
    }

    private String text(JsonNode json, String field) {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String errorCode(RestClientResponseException exception) {
        try {
            JsonNode json = objectMapper.readTree(exception.getResponseBodyAsString());
            String code = text(json, "code");
            if (code == null) {
                code = text(json, "errorCode");
            }
            if (code == null) {
                code = text(json, "error");
            }
            return code == null ? "BATCH_SERVICE_HTTP_" + exception.getStatusCode().value() : code;
        } catch (Exception ignored) {
            return "BATCH_SERVICE_HTTP_" + exception.getStatusCode().value();
        }
    }

    private String errorMessage(RestClientResponseException exception) {
        try {
            JsonNode json = objectMapper.readTree(exception.getResponseBodyAsString());
            String message = text(json, "message");
            if (message == null) {
                message = text(json, "detail");
            }
            return message == null ? exception.getMessage() : message;
        } catch (Exception ignored) {
            return exception.getMessage();
        }
    }

    private String safe(String message) {
        return message == null || message.isBlank() ? "Error tecnico controlado" : message;
    }
}
