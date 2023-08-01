package com.clevertap.android.sdk.cryption;

public abstract class Crypt {
    public EncryptionLevel encryptionLevel;

    protected Crypt(int encryptionLevel) {
        this.encryptionLevel = EncryptionLevel.values()[encryptionLevel];
    }

    protected abstract String generateKeyPassword();

    public abstract String encrypt(String plainText, String key);

    public abstract String decrypt(String cipherText, String key);

    protected abstract byte[] parseCipherText(String cipherText);

    public void setEncryptionLevel(int encryptionLevel) {
        this.encryptionLevel = EncryptionLevel.values()[encryptionLevel];
    }

    public enum EncryptionLevel {
        NONE(0), MEDIUM(1);
        private final int value;

        EncryptionLevel(final int newValue) {
            value = newValue;
        }

        public int intValue() {
            return value;
        }
    }
}
