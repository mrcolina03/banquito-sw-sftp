package com.banquito.switchpagos.sftp.client;

public record BatchUploadResult(
        boolean accepted,
        String batchId,
        String status,
        int httpStatus,
        String errorCode,
        String message,
        String rawResponse) {

    public static BatchUploadResult accepted(String batchId, String status, int httpStatus, String rawResponse) {
        return new BatchUploadResult(true, batchId, status, httpStatus, null, "Batch accepted", rawResponse);
    }

    public static BatchUploadResult rejected(int httpStatus, String errorCode, String message, String rawResponse) {
        return new BatchUploadResult(false, null, null, httpStatus, errorCode, message, rawResponse);
    }
}
