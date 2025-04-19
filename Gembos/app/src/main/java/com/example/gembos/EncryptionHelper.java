package com.example.gembos;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {

    // Hardcoded 32-byte (256-bit) AES key
    private static final byte[] AES_KEY = new byte[] {
            (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67,
            (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF,
            (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67,
            (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF,
            (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67,
            (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF,
            (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67,
            (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
    }; // 32-byte key for AES-256

    private static final String INIT_VECTOR = "1234567890123456"; // 16 bytes IV for AES CBC

    // Return SecretKeySpec using the hardcoded key
    public static SecretKeySpec getSecretKey() {
        return new SecretKeySpec(AES_KEY, "AES");
    }

    // Return IvParameterSpec for AES CBC mode using the fixed IV
    public static IvParameterSpec getIvSpec() {
        return new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
    }

    // Encrypt the plain text using AES CBC
    public static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), getIvSpec());
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return "[GEMBOS]" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Decrypt the encrypted text using AES CBC
    public static String decrypt(String encryptedText) {
        try {
            encryptedText = encryptedText.substring("[GEMBOS]".length()); // Remove prefix
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), getIvSpec());
            byte[] decoded = Base64.decode(encryptedText, Base64.NO_WRAP);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
