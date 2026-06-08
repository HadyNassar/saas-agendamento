package com.hady.saas.infrastructure.security;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia AES-256-GCM para dados sensíveis em repouso.
 *
 * Adicione ao application.properties:
 *   encryption.secret-key=${ENCRYPTION_SECRET_KEY}
 *
 * Adicione ao .env / application-local.yml:
 *   ENCRYPTION_SECRET_KEY=<gere com: openssl rand -base64 32>
 *
 * Formato armazenado no banco: Base64(IV + ciphertext + tag)
 * O IV (12 bytes) é gerado aleatoriamente a cada encrypt — mesmo valor
 * claro gera cifras diferentes, impedindo correlação entre linhas.
 */
@Service
public class AesEncryptionService {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH  = 12;   // bytes — padrão GCM
    private static final int    TAG_LENGTH = 128;  // bits

    @Value("${encryption.secret-key}")
    private String rawKey;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(rawKey);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "ENCRYPTION_SECRET_KEY deve ter exatamente 32 bytes (256 bits). " +
                "Gere com: openssl rand -base64 32"
            );
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Armazena IV + ciphertext concatenados, codificados em Base64
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar dado sensível", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            byte[] iv         = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar dado sensível", e);
        }
    }
}
