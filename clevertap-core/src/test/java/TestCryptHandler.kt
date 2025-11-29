import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.ICryptHandler

internal class TestCryptHandler : ICryptHandler {
    override fun encryptSafe(
        plainText: String,
        isLegacy: Boolean
    ): String? {
        return plainText
    }

    override fun decryptSafe(
        cipherText: String,
        isLegacy: Boolean
    ): String? {
        return cipherText
    }

    override fun encrypt(
        plainText: String,
    ): String? {
        return plainText
    }

    override fun decrypt(
        cipherText: String,
    ): String? {
        return cipherText
    }

    override fun updateMigrationFailureCount(migrationSuccessful: Boolean) {
        println("migration successful=$migrationSuccessful")
    }

    override fun decryptWithAlgorithm(
        cipherText: String,
        algorithm: CryptHandler.EncryptionAlgorithm
    ): String? {
        println("called decrypt with algo")
        return cipherText
    }
}