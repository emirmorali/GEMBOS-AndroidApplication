package com.example.gembos;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyExchangeManager {
    private static final String TAG = "KeyExchangeManager";
    protected static final String PUBLIC_KEY_PREFIX = "[KEYX]";
    private static HashMap<String, SecretKey> sharedKeys = new HashMap<>();
    private static HashMap<String, KeyPair> keyPairs = new HashMap<>();
    private static HashMap<String, PublicKey> receivedPublicKeys = new HashMap<>();
    // HashMap to store received key parts until complete
    private static HashMap<String, StringBuilder> receivedKeyParts = new HashMap<>();

    private static KeyExchangeCallback callback;

    // Set the callback from your activity
    public static void setCallback(KeyExchangeCallback callback) {
        KeyExchangeManager.callback = callback;
    }

    // Generate a new DH Key Pair for a contact
    public static void initiateKeyExchange(Context context, String contact) {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(512);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            keyPairs.put(contact, keyPair);

            // Replace special characters in Base64 to make it safe for SMS
            // When sending the public key
            String publicKeyMessage = PUBLIC_KEY_PREFIX + Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP) + "[KEYX_END]";

            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(publicKeyMessage);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                Intent sentIntent = new Intent("SMS_SENT");
                sentIntent.putExtra("part_index", i);
                PendingIntent sentPI = PendingIntent.getBroadcast(context, i, sentIntent, PendingIntent.FLAG_IMMUTABLE);
                sentIntents.add(sentPI);
            }

            smsManager.sendMultipartTextMessage(contact, null, parts, sentIntents, null);

            Log.d(TAG, "Sent public key " + publicKeyMessage + " to " + contact);

            // Update UI via callback
            if (callback != null) {
                callback.onKeyExchangeMessage("Initiated key exchange with " + contact, true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating key pair: " + e.getMessage());
        }
    }

    // Receive and store the other party's public key
    public static void receivePublicKey(String sender, String receivedKeyBase64) {
        try {
            // Initialize the StringBuilder for the sender if it does not exist
            if (!receivedKeyParts.containsKey(sender)) {
                receivedKeyParts.put(sender, new StringBuilder());
            }

            // Append the received part (whether it starts with [KEYX] or not)
            receivedKeyParts.get(sender).append(receivedKeyBase64);

            // Check for end delimiter
            String fullKeyMessage = receivedKeyParts.get(sender).toString();
            if (fullKeyMessage.contains("[KEYX_END]")) {
                receivedKeyParts.remove(sender); // Clean up once complete
                fullKeyMessage = fullKeyMessage.replace("[KEYX_END]", "");

                // Decode the key (reverse safe Base64)
                String safeBase64Key = fullKeyMessage.replace(PUBLIC_KEY_PREFIX, "");

                // Decoding the received public key
                byte[] otherPublicBytes = Base64.decode(safeBase64Key, Base64.URL_SAFE | Base64.NO_WRAP);

                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(otherPublicBytes);
                PublicKey otherPublicKey = KeyFactory.getInstance("DH").generatePublic(keySpec);
                receivedPublicKeys.put(sender, otherPublicKey);

                KeyPair keyPair;
                // Generate own key pair and send back the public key
                if (!keyPairs.containsKey(sender)) {
                    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
                    keyPairGen.initialize(512);
                    keyPair = keyPairGen.generateKeyPair();
                    keyPairs.put(sender, keyPair);

                    // Send back our public key
                    String responsePublicKey = PUBLIC_KEY_PREFIX + Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP) + "[KEYX_END]";
                    SmsManager smsManager = SmsManager.getDefault();
                    ArrayList<String> parts = smsManager.divideMessage(responsePublicKey);
                    smsManager.sendMultipartTextMessage(sender, null, parts, null, null);

                    Log.d(TAG, "Received complete key and responded with public key" + responsePublicKey + " to " + sender);
                }

                completeKeyExchange(sender);

                // Notify UI that a key was received
                if (callback != null) {
                    callback.onKeyExchangeMessage("Received public key from " + sender, false);
                }
            }
            else {
                Log.d(TAG, "Waiting for complete message.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error receiving public key: " + e.getMessage());
        }
    }

    // Complete the key exchange using the stored private key
    public static void completeKeyExchange(String sender) {
        if (!keyPairs.containsKey(sender) || !receivedPublicKeys.containsKey(sender)) {
            Log.e(TAG, "Key pair or public key not available for " + sender);
            return;
        }

        try {
            KeyPair localKeyPair = keyPairs.get(sender);
            PublicKey otherPublicKey = receivedPublicKeys.get(sender);

            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(localKeyPair.getPrivate());
            keyAgreement.doPhase(otherPublicKey, true);

            byte[] sharedSecret = keyAgreement.generateSecret();
            SecretKey secretKey = EncryptionHelper.deriveAESKey(sharedSecret);
            sharedKeys.put(sender, secretKey);

            Log.d(TAG, "Key exchange completed with key: " + secretKey);

        } catch (Exception e) {
            Log.e(TAG, "Error completing key exchange: " + e.getMessage());
        }
    }

    // Get the shared key for a contact
    public static SecretKey getSharedKey(String phoneNumber) {
        return sharedKeys.get(phoneNumber);
    }

    public static boolean isKeyExchangeInProgress(String sender) {
        return receivedKeyParts.containsKey(sender);
    }
}
