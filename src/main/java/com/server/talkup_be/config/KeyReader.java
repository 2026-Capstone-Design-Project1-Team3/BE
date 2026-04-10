package com.server.talkup_be.config; // 본인의 패키지명으로 변경하세요

import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyReader {

    // 1. Private Key 읽기 (resources 폴더 기준)
    public static PrivateKey getPrivateKey(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource(filename);
            try (InputStream is = resource.getInputStream()) {
                String key = new String(is.readAllBytes());

                String privateKeyPEM = key
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");

                byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
            }
        } catch (Exception e) {
            throw new RuntimeException("Private Key를 읽어오는데 실패했습니다.", e);
        }
    }

    // 2. Public Key 읽기 (resources 폴더 기준)
    public static PublicKey getPublicKey(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource(filename);
            try (InputStream is = resource.getInputStream()) {
                String key = new String(is.readAllBytes());

                String publicKeyPEM = key
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");

                byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
            }
        } catch (Exception e) {
            throw new RuntimeException("Public Key를 읽어오는데 실패했습니다.", e);
        }
    }
}