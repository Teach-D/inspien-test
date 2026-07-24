package com.inspien.eai.transfer;

public interface FileTransferClient {
    void upload(String fileName, String content) throws Exception;
}
