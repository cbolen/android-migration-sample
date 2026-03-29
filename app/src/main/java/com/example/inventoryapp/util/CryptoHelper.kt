package com.example.inventoryapp.util

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility for encrypting and decrypting exported CSV content.
 *
 * Issue: explicitly requests the "BC" (BouncyCastle) provider for cryptographic operations.
 * BouncyCastle implementations of many algorithms were deprecated and then removed in Android 12
 * (API 31). Calling Cipher.getInstance(..., "BC") throws NoSuchProviderException at runtime on
 * API 31+.
 *
 * Fix: remove the provider argument entirely. The default JCA provider on Android (Conscrypt)
 * supports AES/CBC/PKCS5Padding and all other standard algorithms.
 *
 *   val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")  // correct
 */
object CryptoHelper {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val PROVIDER = "BC"

    /**
     * Encrypts [plaintext] bytes using the given [key] and [iv].
     *
     * Issue: Cipher.getInstance(TRANSFORMATION, PROVIDER) explicitly uses BouncyCastle —
     * throws NoSuchProviderException on Android 12+.
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)
        // Issue: explicit "BC" provider — removed in Android 12. Remove the provider argument.
        @Suppress("DEPRECATION")
        val cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(plaintext)
    }

    /**
     * Decrypts [ciphertext] bytes using the given [key] and [iv].
     *
     * Issue: same BouncyCastle provider problem as encrypt().
     */
    fun decrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)
        // Issue: explicit "BC" provider — removed in Android 12. Remove the provider argument.
        @Suppress("DEPRECATION")
        val cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(ciphertext)
    }
}
