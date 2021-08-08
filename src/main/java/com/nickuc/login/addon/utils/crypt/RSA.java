/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.utils.crypt;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSA {

    @Nullable
    public static PublicKey getPublicKeyFromBytes(byte[] bytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    @Nullable
    public static byte[] decrypt(PublicKey publicKey, byte[] data) {
        try {
            Cipher decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(2, publicKey);
            return decryptCipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

}
