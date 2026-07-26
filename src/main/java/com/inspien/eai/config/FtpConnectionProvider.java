package com.inspien.eai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FtpConnectionProvider {

    private final ConfigApiClient configApiClient;
    private final String applicantName;
    private final String applicantEmail;

    private volatile FtpConnectionInfo cached;

    public FtpConnectionProvider(
            ConfigApiClient configApiClient,
            @Value("${eai.applicant-name}") String applicantName,
            @Value("${eai.applicant-email}") String applicantEmail) {
        this.configApiClient = configApiClient;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
    }

    public synchronized FtpConnectionInfo get() {
        if (cached == null) {
            try {
                ConfigResponse config = configApiClient.fetchConfig(applicantName, applicantEmail);
                cached = configApiClient.resolveFtpConnection(config);
            } catch (Exception e) {
                throw new IllegalStateException("FTP 접속정보 조회/복호화 실패: " + e.getMessage(), e);
            }
        }
        return cached;
    }

    public synchronized void invalidate() {
        cached = null;
    }
}
