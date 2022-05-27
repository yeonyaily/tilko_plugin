package com.swfact.tilko_plugin;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import at.favre.lib.bytes.Bytes;

/**
 * @FileName : Util.java
 * @Project : TilkoSampleSource
 * @Date : 2020. 8. 13.
 * @작성자 : Tilko.net
 * @변경이력 : yejin(RSA -> RSA/None/PKCS1Padding)
 * @프로그램 설명 : 유틸리티 모음
 */
public class Util {
    static final Logger logger = LoggerFactory.getLogger(Util.class);

    //	File을 byte[]로 변환
    static public byte[] FileToByteArray(String filePath) throws IOException {
        File file = new File(filePath);
        // init array with file length
        byte[] bytesArray = new byte[(int) file.length()];

        FileInputStream fis = new FileInputStream(file);
        fis.read(bytesArray); // read file into bytes[]
        fis.close();

        return bytesArray;
    }

    //	RSA공개키로 암호

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[] encodeByRSAPublicKey(byte[] plainData, String pubKeyBlob) {
//		String encryptedData = null;
        byte[] byteEncryptedData = null;

        try {

            // [20210204, Joshua] 기존 X.509 개체 활용 방법
//			logger.info("pubKeyBlob:{}", pubKeyBlob);
//			logger.info("plainData:{}", base64Encode(plainData));
//			X509Certificate x509Cert = X509Certificate.getInstance(base64Decode(pubKeyBlob));
//			RSAPublicKey key = (RSAPublicKey) x509Cert.getPublicKey();
//			logger.info("modulus int : {}", key.getModulus());
//			logger.info("exponent int : {}", key.getPublicExponent());

            // [20210204, Joshua] RSA Exponent와 Modulus 이용하는 방법으로 변경
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] keyBytes = Base64.getDecoder().decode(pubKeyBlob.getBytes("UTF-8"));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey fileGeneratedPublicKey = keyFactory.generatePublic(spec);
            RSAPublicKey key  = (RSAPublicKey)(fileGeneratedPublicKey);
//            BigInteger publicKeyModulus = key.getModulus();
//            BigInteger publicKeyExponent  = key.getPublicExponent();
//            String nModulus=Base64.getUrlEncoder().encodeToString(publicKeyModulus.toByteArray());
//            String eExponent=Base64.getUrlEncoder().encodeToString(publicKeyExponent.toByteArray());

            // 만들어진 공개키객체를 기반으로 암호화모드로 설정하는 과정
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // 평문을 암호화하는 과정
            byteEncryptedData = cipher.doFinal(plainData);

            return byteEncryptedData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return byteEncryptedData;
    }

    static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for (final byte b : a)
            sb.append(String.format("%02x ", b & 0xff));

        return sb.toString();
    }

    static String getExpStr(String source) {
        byte[] keyBytes = base64Decode(source);
        Bytes b = Bytes.wrap(keyBytes);
        String result = b.copy(16, 4).reverse().encodeHex();

        return result;
    }

    static String getModulusStr(String source) {
        byte[] keyBytes = base64Decode(source);
        Bytes b = Bytes.wrap(keyBytes);
        String modulusStr = "";

        int eightBitLen = b.copy(12, 4).reverse().toInt() / 8;
        modulusStr = b.copy(20, eightBitLen).reverse().encodeHex();

        return modulusStr;
    }

    // base64 encode
    public static String base64Encode(byte[] plainBytes) {
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(plainBytes));
    }

    public static String base64Encode(String plainBytes, String encoding) throws UnsupportedEncodingException {
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(plainBytes.getBytes(encoding)));
    }

    // base64 decode
    static byte[] base64Decode(String plainText) {
        return org.apache.commons.codec.binary.Base64.decodeBase64(plainText);
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }

//	public static PublicKey getRSAPublicKey(String rsa_cert) throws CertificateException {
//		X509Certificate x509Cert = X509Certificate.getInstance(rsa_cert.getBytes());
//		PublicKey publicKey = x509Cert.getPublicKey();
//
//		return publicKey;
//	}

    public static byte[] RSA_encrypt(PublicKey key, byte[] plaintext) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(plaintext);
    }

}