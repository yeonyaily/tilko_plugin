package com.swfact.tilko_plugin;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @FileName : AES.java
 * @Project : TilkoSampleSource
 * @Date : 2020. 8. 13.
 * @작성자 : Tilko.net
 * @변경이력 :
 * @프로그램 설명 : AES 암호화
 */
public class AES {

    private byte[] Key;

    public final byte[] getKey() {
        return Key;
    }

    public final void setKey(byte[] value) {
        Key = value;
    }

    private byte[] Iv;

    public final byte[] getIv() {
        return Iv;
    }

    public final void setIv(byte[] value) {
        Iv = value;
    }

    public final byte[] Encrypt(byte[] PlainText) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");	//JAVA의 PKCS5Padding은 PKCS7Padding과 호환
        SecretKeySpec keySpec = new SecretKeySpec(Key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(Iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(PlainText);

    }

}