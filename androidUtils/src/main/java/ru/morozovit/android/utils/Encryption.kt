@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * Encrypts the string using AES with a password-derived key.
 * @param password The password to use for encryption.
 * @return The encrypted string, including salt and IV.
 */
fun String.encrypt(password: String) = Cipher.getInstance("AES/CBC/PKCS5Padding").let {
    val salt = ByteArray(16).also { a -> SecureRandom().nextBytes(a) }
    val iv = ByteArray(it.blockSize).also { a -> SecureRandom().nextBytes(a) }

    it.init(
        Cipher.ENCRYPT_MODE,
        SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(
                PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    65536,
                    256
                )
            ),
        IvParameterSpec(iv)
    )

    "${Base64.encodeToString(salt, Base64.DEFAULT)}:" +
            "${Base64.encodeToString(iv, Base64.DEFAULT)}:" +
            "${Base64.encodeToString(it.doFinal(toByteArray()), Base64.DEFAULT)}"
}

/**
 * Decrypts a string encrypted with [encrypt].
 * @param password The password to use for decryption.
 * @return The decrypted string.
 */
fun String.decrypt(password: String) = Cipher.getInstance("AES/CBC/PKCS5Padding").let {
    val parts = split(":")

    it.init(
        Cipher.DECRYPT_MODE,
        SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(
                PBEKeySpec(
                    password.toCharArray(),
                    Base64.decode(parts[0], Base64.DEFAULT),
                    65536,
                    256
                )
            ),
        IvParameterSpec(Base64.decode(parts[1], Base64.DEFAULT))
    )

    String(it.doFinal(Base64.decode(parts[2], Base64.DEFAULT)))
}

/**
 * Computes the SHA-256 hash of the string and encodes it as Base64.
 * @return The Base64-encoded hash.
 */
fun String.hash() = Base64.encodeToString(
    MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray()),
    Base64.NO_WRAP
)!!

/**
 * Checks if the hash of this string matches the given hash.
 * @param hash The hash to compare to.
 * @return True if the hashes match, false otherwise.
 */
inline fun String.checkHash(hash: String) = hash() == hash