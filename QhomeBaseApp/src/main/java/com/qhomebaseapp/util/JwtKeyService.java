package com.qhomebaseapp.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Base64;
import java.security.MessageDigest;

@Slf4j
@Component
public class JwtKeyService {

    private volatile PrivateKey privateKey;
    private volatile PublicKey publicKey;
    private volatile String currentKid;

    @Value("${jwt.key.active-kid:default}")
    private String activeKid;

    @Value("${jwt.keys[0].private-key-pem:#{null}}")
    private String privateKeyPem;

    @Value("${jwt.keys[0].public-key-pem:#{null}}")
    private String publicKeyPem;

    @Value("${jwt.additional-public-keys:#{null}}")
    private String additionalPublicKeysProp;

    private final Map<String, PublicKey> publicKeyMap = new ConcurrentHashMap<>();

    private synchronized void ensureLoaded() {
        if (currentKid != null) return;

        try {
            if (privateKeyPem != null && publicKeyPem != null) {
                this.privateKey = parsePrivateKeyFromPem(privateKeyPem);
                this.publicKey = parsePublicKeyFromPem(publicKeyPem);
                this.currentKid = activeKid;
                publicKeyMap.put(currentKid, publicKey);
                log.info("Loaded active JWT key: {}", currentKid);
            }

            if (additionalPublicKeysProp != null && !additionalPublicKeysProp.isBlank()) {
                String[] entries = additionalPublicKeysProp.split(";;");
                for (String entry : entries) {
                    String trimmed = entry.trim();
                    if (trimmed.isEmpty()) continue;
                    int idx = trimmed.indexOf(':');
                    if (idx <= 0) continue;
                    String kid = trimmed.substring(0, idx);
                    String pemBase64 = trimmed.substring(idx + 1);
                    try {
                        String pem = new String(Base64.getDecoder().decode(pemBase64));
                        PublicKey pk = parsePublicKeyFromPem(pem);
                        publicKeyMap.put(kid, pk);
                        log.info("Loaded additional public key: {}", kid);
                    } catch (Exception e) {
                        log.warn("Failed to parse additional key {}: {}", kid, e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT keys", e);
        }
    }

    public synchronized void rotateKeys() {
        this.privateKey = null;
        this.publicKey = null;
        this.currentKid = null;
        this.publicKeyMap.clear();
        ensureLoaded();
    }

    public String getCurrentKid() {
        ensureLoaded();
        return currentKid;
    }

    public PrivateKey getPrivateKey(String kid) {
        ensureLoaded();
        if (!Objects.equals(currentKid, kid)) {
            throw new IllegalArgumentException("Private key for kid not available: " + kid);
        }
        return privateKey;
    }

    public PublicKey getPublicKeyByKid(String kid) {
        ensureLoaded();
        PublicKey pk = publicKeyMap.get(kid);
        if (pk == null) {
            throw new IllegalArgumentException("Public key not found for kid: " + kid);
        }
        return pk;
    }

    public PublicKey getCurrentPublicKey() {
        ensureLoaded();
        return publicKey;
    }

    private PrivateKey parsePrivateKeyFromPem(String pem) {
        try {
            String cleaned = pem.replaceAll("-----BEGIN ([A-Z ]+)-----", "")
                    .replaceAll("-----END ([A-Z ]+)-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse private key", e);
        }
    }

    private PublicKey parsePublicKeyFromPem(String pem) {
        try {
            String cleaned = pem.replaceAll("-----BEGIN ([A-Z ]+)-----", "")
                    .replaceAll("-----END ([A-Z ]+)-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse public key", e);
        }
    }
}
