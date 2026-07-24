package com.inspien.eai.config;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public final class DecryptionUtil {

    private static final String CIPHER_ALGO = "AES/ECB/PKCS5Padding";

    private DecryptionUtil() {
    }

    public static String decryptToUtf8(String base64Cipher, String phoneNumber) throws Exception {
        byte[] plain = decrypt(base64Cipher, phoneNumber);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static byte[] decrypt(String base64Cipher, String phoneNumber) throws Exception {
        byte[] key = deriveKey(phoneNumber);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] encrypted = Base64.getDecoder().decode(base64Cipher);
        return cipher.doFinal(encrypted);
    }

    private static byte[] deriveKey(String phoneNumber) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest(phoneNumber.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOfRange(digest, 0, 16);
    }
}
