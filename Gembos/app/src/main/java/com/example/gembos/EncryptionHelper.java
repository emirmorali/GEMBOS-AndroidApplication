package com.example.gembos;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    // Derive AES key from the shared secret
    public static SecretKeySpec deriveAESKey(byte[] sharedSecret) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] key = sha256.digest(sharedSecret);
        return new SecretKeySpec(key, 0, 32, "AES");
    }

    // Encrypt text using AES CBC with dynamic IV
    public static String encrypt(String plainText, SecretKeySpec keySpec) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        String ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP);
        String encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP);

        return "[GEMBOS]" + ivBase64 + ":" + encryptedBase64;
    }

    // Decrypt text using AES CBC with dynamic IV
    public static String decrypt(String encryptedText, SecretKeySpec keySpec) throws Exception {
        encryptedText = encryptedText.substring("[GEMBOS]".length());
        String[] parts = encryptedText.split(":");
        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

        byte[] decrypted = cipher.doFinal(encryptedBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // Verify if a text is encrypted with Gembos prefix
    public static boolean isEncrypted(String text) {
        return text.startsWith("[GEMBOS]");
    }
}
