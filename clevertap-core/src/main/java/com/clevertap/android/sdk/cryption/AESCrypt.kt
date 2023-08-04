package com.clevertap.android.sdk.cryption;

import static com.clevertap.android.sdk.Constants.CRYPTION_IV;
import static com.clevertap.android.sdk.Constants.CRYPTION_SALT;
import static com.clevertap.android.sdk.Constants.MEDIUM_CRYPT_KEYS;
import static com.clevertap.android.sdk.Constants.NONE_CRYPT_KEYS;

import com.clevertap.android.sdk.Logger;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCrypt extends Crypt {

    // Build prefix and suffix strings longhand, to obfuscate them slightly.
    // Probably doesn't matter.
    private static final String APP_ID_KEY_PREFIX = new StringBuilder()
            .append("L").append("q").append(3).append("f").append("z").toString();
    private static final String APP_ID_KEY_SUFFIX = new StringBuilder()
            .append("b").append("L").append("t").append("i").append(2).toString();
    private final String accountID;

    public AESCrypt(String accountID, int encryptionLevel) {
        super(encryptionLevel);
        this.accountID = accountID;
    }

    /**
     * This method returns the key-password to be used for encryption/decryption
     *
     * @return key-password
     */
    @Override
    protected String generateKeyPassword() {
        return APP_ID_KEY_PREFIX + accountID + APP_ID_KEY_SUFFIX;
    }

    /**
     * This method returns the encrypted text if the key is a part of the current encryption level
     *
     * @param plainText - plainText to be encrypted
     * @param key       - key of the plainText to be encrypted
     * @return encrypted text
     */
    @Override
    public String encrypt(String plainText, String key) {
        switch (encryptionLevel) {
            case MEDIUM:
                if (MEDIUM_CRYPT_KEYS.contains(key)) return encryptInternal(plainText);
            default:
                return plainText;
        }
    }

    /**
     * This method is used internally to encrypt the plain text
     *
     * @param plainText - plainText to be encrypted
     * @return encrypted text
     */
    private String encryptInternal(String plainText) {
        return Arrays.toString(performCryptOperation(Cipher.ENCRYPT_MODE, generateKeyPassword(), plainText.getBytes(StandardCharsets.UTF_8)));
    }


    /**
     * This method returns the decrypted text if the key is a part of the current encryption level
     *
     * @param cipherText - cipherText to be decrypted
     * @param key        - key of the cipherText that needs to be decrypted
     * @return decrypted text
     */
    @Override
    public String decrypt(String cipherText, String key) {
        String decrypted = cipherText;
        // None crypt keys is required in the case of migration
        switch (encryptionLevel) {
            case MEDIUM:
                if (MEDIUM_CRYPT_KEYS.contains(key))
                    decrypted = decryptInternal(cipherText);
            default:
                if (NONE_CRYPT_KEYS.contains(key))
                    decrypted = decryptInternal(cipherText);
        }
        if (decrypted != null)
            return decrypted;
        else return cipherText;
    }

    /**
     * This method is used internally to decrypt the cipher text
     *
     * @param cipherText - cipherText to be decrypted
     * @return decrypted text
     */
    private String decryptInternal(String cipherText) {
        byte[] bytes = parseCipherText(cipherText);
        if (bytes == null)
            return null;
        byte[] byteResult = performCryptOperation(Cipher.DECRYPT_MODE, generateKeyPassword(), bytes);
        return new String(byteResult, StandardCharsets.UTF_8);
    }


    /**
     * This method is used to parse the cipher text (i.e remove the [] and split the string around commas) and convert it to a byte array during decryption
     *
     * @param cipherText - cipherext to be parsed
     * @return Parsed string in the form of a byte array
     */
    @Override
    protected byte[] parseCipherText(String cipherText) {
        try {
            String[] byteStrings = cipherText.substring(1, cipherText.length() - 1).trim().split("\\s*,\\s*");
            byte[] bytes = new byte[byteStrings.length];
            for (int i = 0; i < byteStrings.length; i++) {
                bytes[i] = Byte.parseByte(byteStrings[i]);
            }
            return bytes;
        } catch (NumberFormatException e) {
            Logger.v(this.accountID, "Unable to parse cipher text");
            return null;
        }
    }

    /**
     * This method actually performs both the encryption and decryption crypt task.
     *
     * @param mode     - mode to determine encryption/decryption
     * @param password - password for cryption
     * @param text     - text to be crypted
     * @return Crypted text in the form of a byte array
     */
    private byte[] performCryptOperation(int mode, String password, byte[] text) {
        byte[] result = null;
        try {
            byte[] SALT = CRYPTION_SALT.getBytes(StandardCharsets.UTF_8);
            byte[] IV = CRYPTION_IV.getBytes(StandardCharsets.UTF_8);
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), SALT, 1000,
                    256);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5And128BitAES-CBC-OpenSSL");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParams = new IvParameterSpec(IV);
            cipher.init(mode, key, ivParams);

            result = cipher.doFinal(text);
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException |
                 BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException | NoSuchPaddingException e) {
            Logger.v(this.accountID, "Unable to perform crypt operation", e);
        }
        return result;
    }
}
