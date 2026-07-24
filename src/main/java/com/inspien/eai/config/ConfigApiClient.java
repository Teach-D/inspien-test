package com.inspien.eai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ConfigApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String phoneNumber;

    public ConfigApiClient(
            @Value("${eai.recruiting-api.url}") String apiUrl,
            @Value("${eai.recruiting-api.username}") String username,
            @Value("${eai.recruiting-api.password}") String password,
            @Value("${eai.phone-number}") String phoneNumber,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestInterceptor(new BasicAuthenticationInterceptor(username, password))
                .build();
        this.objectMapper = objectMapper;
        this.phoneNumber = phoneNumber;
    }

    public ConfigResponse fetchConfig(String name, String email) {
        String rawBody = restClient.post()
                .body(Map.of(
                        "NAME", name,
                        "PHONE_NUMBER", phoneNumber,
                        "E_MAIL", email))
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(rawBody, ConfigResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("RecruitingTest 응답 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    public FtpConnectionInfo resolveFtpConnection(ConfigResponse config) throws Exception {
        Map<String, String> ftpConn = config.ftpConn();
        String host = DecryptionUtil.decryptToUtf8(ftpConn.get("URL"), phoneNumber);
        int port = Integer.parseInt(DecryptionUtil.decryptToUtf8(ftpConn.get("PORT"), phoneNumber));
        String username = DecryptionUtil.decryptToUtf8(ftpConn.get("ID"), phoneNumber);
        String password = DecryptionUtil.decryptToUtf8(ftpConn.get("PASSWORD"), phoneNumber);
        String remoteDir = DecryptionUtil.decryptToUtf8(ftpConn.get("PATH"), phoneNumber);
        return new FtpConnectionInfo(host, port, username, password, remoteDir);
    }

    public String decodeSampleData(ConfigResponse config) throws Exception {
        byte[] decoded = java.util.Base64.getDecoder().decode(config.sampleData());
        return new String(decoded, "EUC-KR");
    }
}
