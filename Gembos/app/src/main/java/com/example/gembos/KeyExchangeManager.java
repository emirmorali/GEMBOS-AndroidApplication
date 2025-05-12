package com.example.gembos;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyExchangeManager {
    private static final String TAG = "KeyExchangeManager";
    private static final String KEY_ALIAS = "GEMBOS_KEY";
    protected static final String PUBLIC_KEY_PREFIX = "[KEYX]";
    private static HashMap<String, SecretKey> sharedKeys = new HashMap<>();
    private Context context;

    public KeyExchangeManager(Context context) {
        this.context = context;
    }

    public String generateAndEncodePublicKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Save private key
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.PrivateKeyEntry entry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new java.security.cert.Certificate[]{});
            // You may store the private key securely here, or hold it in memory if needed (not recommended for production)

            // Return encoded public key
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
            return PUBLIC_KEY_PREFIX + Base64.encodeToString(publicKeyBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error generating key pair: ", e);
            return null;
        }
    }

    public void receivePublicKey(String sender, String encodedKey) {
        try {
            String base64Key = encodedKey.replace(PUBLIC_KEY_PREFIX, "").trim();
            byte[] otherPublicBytes = Base64.decode(base64Key, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(otherPublicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            PublicKey otherPublicKey = keyFactory.generatePublic(keySpec);

            // Generate our key pair using the same params
            DHParameterSpec dhSpec = ((DHPublicKey) otherPublicKey).getParams();
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(dhSpec);
            KeyPair keyPair = keyPairGen.generateKeyPair();

            // Perform key agreement
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(otherPublicKey, true);
            byte[] sharedSecret = keyAgreement.generateSecret();

            SecretKey secretKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            sharedKeys.put(sender, secretKey);
            Log.d(TAG, "Shared key established with " + sender);

        } catch (Exception e) {
            Log.e(TAG, "Error receiving public key: ", e);
        }
    }

    public SecretKey getSharedKey(String phoneNumber) {
        return sharedKeys.get(phoneNumber);
    }

    public static boolean isKeyExchangeMessage(String message) {
        return message.startsWith(PUBLIC_KEY_PREFIX);
    }
}
