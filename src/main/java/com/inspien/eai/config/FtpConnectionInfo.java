package com.inspien.eai.config;

public record FtpConnectionInfo(
        String host,
        int port,
        String username,
        String password,
        String remoteDir
) {
}
