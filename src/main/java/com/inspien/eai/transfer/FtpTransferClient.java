package com.inspien.eai.transfer;

import com.inspien.eai.config.FtpConnectionInfo;
import com.inspien.eai.config.FtpConnectionProvider;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.PrintCommandListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Component
public class FtpTransferClient implements FileTransferClient {

    private final FtpConnectionProvider connectionProvider;
    private final boolean debugLog;
    private final int connectTimeoutMs;

    public FtpTransferClient(
            FtpConnectionProvider connectionProvider,
            @Value("${eai.ftp.debug-log:true}") boolean debugLog,
            @Value("${eai.ftp.connect-timeout-ms:10000}") int connectTimeoutMs) {
        this.connectionProvider = connectionProvider;
        this.debugLog = debugLog;
        this.connectTimeoutMs = connectTimeoutMs;
    }

    @Override
    public void upload(String fileName, String content) throws Exception {
        FtpConnectionInfo conn = connectionProvider.get();
        System.out.println("[FTP] 연결 시도 host=" + conn.host() + " port=" + conn.port()
                + " user=" + conn.username() + " remoteDir=" + conn.remoteDir());

        FTPClient ftp = new FTPClient();
        ftp.setConnectTimeout(connectTimeoutMs);
        ftp.setDataTimeout(java.time.Duration.ofMillis(connectTimeoutMs));
        ftp.setDefaultTimeout(connectTimeoutMs);

        if (debugLog) {
            ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
        }

        try {
            ftp.connect(conn.host(), conn.port());
            int reply = ftp.getReplyCode();
            System.out.println("[FTP] connect() 응답 코드=" + reply);
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IllegalStateException("FTP 서버 연결 거부: reply=" + reply);
            }

            boolean loginOk = ftp.login(conn.username(), conn.password());
            System.out.println("[FTP] login() 결과=" + loginOk);
            if (!loginOk) {
                connectionProvider.invalidate();
                throw new IllegalStateException("FTP 로그인 실패: user=" + conn.username());
            }

            ftp.enterLocalPassiveMode();
            ftp.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            if (conn.remoteDir() != null && !conn.remoteDir().isBlank()) {
                boolean cdOk = ftp.changeWorkingDirectory(conn.remoteDir());
                System.out.println("[FTP] changeWorkingDirectory(" + conn.remoteDir() + ") 결과=" + cdOk);
                if (!cdOk) {
                    throw new IllegalStateException(
                            "FTP 원격 디렉토리 이동 실패: " + conn.remoteDir() + " reply=" + ftp.getReplyCode());
                }
            }

            try (var in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
                boolean ok = ftp.storeFile(fileName, in);
                System.out.println("[FTP] storeFile(" + fileName + ") 결과=" + ok + " replyCode=" + ftp.getReplyCode());
                if (!ok) {
                    throw new IllegalStateException("FTP 파일 전송 실패: " + fileName + " reply=" + ftp.getReplyCode());
                }
            }
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.logout();
                    ftp.disconnect();
                }
            } catch (Exception ignore) {
            }
        }
    }
}
