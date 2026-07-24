package com.inspien.eai.factory;

import com.inspien.eai.transfer.FileTransferClient;
import com.inspien.eai.transfer.FtpTransferClient;
import org.springframework.stereotype.Component;

@Component
public class TransferClientFactory {

    private final FtpTransferClient ftpTransferClient;

    public TransferClientFactory(FtpTransferClient ftpTransferClient) {
        this.ftpTransferClient = ftpTransferClient;
    }

    public FileTransferClient create(String protocol) {
        return switch (protocol.toUpperCase()) {
            case "FTP" -> ftpTransferClient;
            default -> throw new IllegalArgumentException("지원하지 않는 전송 프로토콜: " + protocol);
        };
    }
}
