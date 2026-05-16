package com.server.talkup_be.config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyReader {

    // 1. Private Key 읽기 (절대 경로 기준)
    public static PrivateKey getPrivateKey(String filePath) {
        try {
            // 절대 경로에서 직접 읽기
            String key = Files.readString(Paths.get(filePath));

            String privateKeyPEM = key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));

        } catch (Exception e) {
            throw new RuntimeException("Private Key를 읽어오는데 실패했습니다. 경로: " + filePath, e);
        }
    }

    // 2. Public Key 읽기 (절대 경로 기준)
    public static PublicKey getPublicKey(String filePath) {
        try {
            String key = Files.readString(Paths.get(filePath));

            String publicKeyPEM = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));

        } catch (Exception e) {
            throw new RuntimeException("Public Key를 읽어오는데 실패했습니다. 경로: " + filePath, e);
        }
    }
}