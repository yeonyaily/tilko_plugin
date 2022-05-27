package com.swfact.tilko_plugin;

import com.google.gson.Gson;
import com.swfact.tilko_plugin.model.RsaPublicKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//import tilko.api.models.RsaPublicKey;

/**
 * @FileName : APIHelper.java
 * @Project : TilkoSampleSource
 * @Date : 2020. 8. 13.
 * @작성자 : Tilko.net
 * @변경이력 :
 * @프로그램 설명 :	API 요청 Helper Class
 */
public class APIHelper {
    static final Logger _logger = LoggerFactory.getLogger(APIHelper.class);

    //	Tilko.net에서 발급된 공용키
    static String _pubUrl = "https://api.tilko.net/api/Auth/GetPublicKey?ApiKey=";
    //  API 호출 공동 부분
    static String _apiHeader = "https://api.tilko.net/";

    static String _paymentUrl = "";
    static String _myDrugUrl = "";

    //	Tilko.net의 API 와 통신간 파라미터에 담긴 사용자의 개인정보를 암호화하기 위한 AES 알고리즘
    private AES _aes;
    //	Tilko.net 에 서 발급된 개인 apiKey
    private String _apiKey;


    public APIHelper(String apiKey) {
        this._apiKey = apiKey;

        //	AES 초기화
        this._aes = new AES();
        byte[] _aesPlainKey = new byte[16];
        new Random().nextBytes(_aesPlainKey);

        this._aes.setKey(_aesPlainKey);
        this._aes.setIv(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
    }

    /**
     * @return : AES키
     * @Method Name : getAESPlainKey
     * @작성일 : 2020. 8. 13.
     * @작성자 : Tilko.net
     * @변경이력 :
     * @Method 설명 : AES키 호출
     */
    public byte[] getAESPlainKey() {

        return this._aes.getKey();
    }

    /**
     * @return : RSA공개키
     * @throws IOException
     * @Method Name : getRSAPubKey
     * @작성일 : 2020. 8. 13.
     * @작성자 : tilko.net
     * @변경이력 :
     * @Method 설명 :RSA공개키 요청 API호출
     */
    public RsaPublicKey getRSAPubKey() throws IOException {
        OkHttpClient _client = new OkHttpClient();
        Request _request = new Request.Builder()
                .url(_pubUrl + this._apiKey)
                .header("Content-Type", "application/json")
                .build();
        Response _response = _client.newCall(_request).execute();
        String _responseStr = _response.body().string();
        RsaPublicKey _pubKey = (RsaPublicKey) new Gson().fromJson(_responseStr, RsaPublicKey.class);

//        _logger.info("========= RSA KEY REQUEST =========");
//        _logger.info("Response(RSA) :" + _responseStr);
//        _logger.info("APIKEY:" + _pubKey.getApiKey());
//        _logger.info("RSA PublicKey:" + _pubKey.getPublicKey());
//		_logger.info("Message:" + _pubKey.getMessage());

        return _pubKey;
    }

    /**
     * @param aesCipherKey      : ENC-Key
     * @param certFilePath      : 인증서 공용키
     * @param keyFilePath       : 인증서 개인키
     * @param txtIdentityNumber : 주민등록번호(202008131234567)
     * @param txtCertPassword   : 인증서 암호
     * @param year              : 검색년도(yyyy)
     * @param startMonth        : 검색 시작 월(MM)
     * @param endMonth          : 검색 종료 월(MM)
     * @return : 결과
     * @throws IOException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @Method Name : getPaymentList
     * @작성일 : 2020. 8. 13.
     * @작성자 : Tilko.net
     * @변경이력 :
     * @Method 설명 : 건강보험료납부내역 API 호출
     */
    public String getPaymentList(byte[] aesCipherKey, String certFilePath, String keyFilePath, String txtIdentityNumber, String txtCertPassword, String year, String startMonth, String endMonth) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        _logger.info(certFilePath);
        _logger.info(keyFilePath);
        _logger.info(txtIdentityNumber);
        _logger.info(txtCertPassword);

        byte[] _certCipherBytes = this._aes.Encrypt(Util.FileToByteArray(certFilePath));
        byte[] _keyCipherBytes = this._aes.Encrypt(Util.FileToByteArray(keyFilePath));
        byte[] _identityCipherBytes = this._aes.Encrypt(txtIdentityNumber.replace("-", "").getBytes("US-ASCII"));
        byte[] _passwordCipherBytes = this._aes.Encrypt(txtCertPassword.getBytes("US-ASCII"));

        //	파라미터 셋팅
        HashMap<String, String> _bodyMap = new HashMap<String, String>();
        _bodyMap.put("CertFile", Util.base64Encode(_certCipherBytes));
        _bodyMap.put("KeyFile", Util.base64Encode(_keyCipherBytes));
        _bodyMap.put("IdentityNumber", Util.base64Encode(_identityCipherBytes));
        _bodyMap.put("CertPassword", Util.base64Encode(_passwordCipherBytes));
        _bodyMap.put("Year", year);
        _bodyMap.put("StartMonth", startMonth);
        _bodyMap.put("EndMonth", endMonth);

        RequestBody _body = RequestBody.create
                (
                        MediaType.parse("application/json; charset=utf-8"),
                        new Gson().toJson(_bodyMap)
                );

        OkHttpClient _client = new OkHttpClient().newBuilder().build();
        Request _request = new Request.Builder()
                .url(APIHelper._paymentUrl)
                .post(_body)
                .addHeader("Content-Type", "application/json")
                .addHeader("API-Key", this._apiKey)
                .addHeader("ENC-Key", Util.base64Encode(aesCipherKey))
                .build();

        Response _response = _client.newCall(_request).execute();
        String _responseStr = _response.body().string();

        // _logger.info("========= PaymentList REQUEST =========");
        // _logger.info("Response : " + _responseStr);
        // _logger.info("API-Key: " + this._apiKey);
        // _logger.info("ENC-Key: {}", Util.base64Encode(aesCipherKey));

        return _responseStr;
    }


    /**
     * @param aesCipherKey      : ENC-Key
     * @param certFilePath      : 인증서 공개키
     * @param keyFilePath       : 인증서 개인키
     * @param txtIdentityNumber : 주민등록번호
     * @param txtCertPassword   : 인증서 비밀번호
     * @param telecomCompany    : 통신사 코드 (통신사 SKT : 0 / KT : 1 / LGT : 2 / SKT알뜰폰 : 3 / KT알뜰폰 : 4 / LGT알뜰폰 : 5 / NA : 6)
     * @param cellphoneNumber   : 휴대폰 번호
     * @return : 결과
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     * @Method Name : getMYDrug
     * @작성일 : 2020. 8. 13.
     * @작성자 : Tilko.net
     * @변경이력 :
     * @Method 설명 : 내가 먹는 약 API 호출
     */
    public String getMYDrug(byte[] aesCipherKey, String certFilePath, String keyFilePath, String txtIdentityNumber, String txtCertPassword, String telecomCompany, String cellphoneNumber) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {

        byte[] _certCipherBytes = this._aes.Encrypt(Util.FileToByteArray(certFilePath));
        byte[] _keyCipherBytes = this._aes.Encrypt(Util.FileToByteArray(keyFilePath));
        byte[] _identityCipherBytes = this._aes.Encrypt(txtIdentityNumber.replace("-", "").getBytes("US-ASCII"));
        byte[] _passwordCipherBytes = this._aes.Encrypt(new String(txtCertPassword).getBytes("US-ASCII"));
        byte[] _cellphonNumberCipherByte = this._aes.Encrypt(cellphoneNumber.getBytes("US-ASCII"));

        _logger.info("========= MY_Drug REQUEST =========");
        _logger.info(" ");

        HashMap<String, String> _bodyMap = new HashMap<String, String>();
        _bodyMap.put("CertFile", Util.base64Encode(_certCipherBytes));
        _bodyMap.put("KeyFile", Util.base64Encode(_keyCipherBytes));
        _bodyMap.put("IdentityNumber", Util.base64Encode(_identityCipherBytes));
        _bodyMap.put("CertPassword", Util.base64Encode(_passwordCipherBytes));
        _bodyMap.put("TelecomCompany", telecomCompany);
        _bodyMap.put("CellphoneNumber", Util.base64Encode(_cellphonNumberCipherByte));

        RequestBody _body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new Gson().toJson(_bodyMap));

        OkHttpClient _client = new OkHttpClient().newBuilder().build();
        Request _request = new Request.Builder()
                .url(APIHelper._myDrugUrl)
                .post(_body)
                .addHeader("Content-Type", "application/json")
                .addHeader("API-Key", this._apiKey)
                .addHeader("ENC-Key", Util.base64Encode(aesCipherKey))
                .build();

        Response _response = _client.newCall(_request).execute();
        String _responseStr = _response.body().string();

        // _logger.info("response:" + _response.toString());
        // _logger.info("responseStr:" + _responseStr.toString());

        // _logger.info("========= MY_Drug REQUEST =========");
        // _logger.info(" ");
        // _logger.info("Response :" + _responseStr);
        // _logger.info("API-Key:" + _apiKey);
        // _logger.info(" ");
        // _logger.info("ENC-Key:{}", Util.base64Encode(aesCipherKey));
        // _logger.info(" ");
        // _logger.info("CertFile:" + Util.base64Encode(_certCipherBytes));
        // _logger.info(" ");
        // _logger.info("KeyFile:" + Util.base64Encode(_keyCipherBytes));
        // _logger.info(" ");
        // _logger.info("IdentityNumber:" + Util.base64Encode(_identityCipherBytes));
        // _logger.info(" ");
        // _logger.info("CertPassword:" + Util.base64Encode(_passwordCipherBytes));
        // _logger.info(" ");

        return _responseStr;
    }

    /**
     * @param aesCipherKey    : ENC-Key
     * @param certFilePath    : 인증서 공개키
     * @param keyFilePath     : 인증서 개인키
     * @param txtCertPassword : 인증서 비밀번호
     * @Method Name : getMedicalCheckInfo
     * @작성일 : 2021. 8. 24.
     * @작성자 : HEM(yechan, yejin, yxxnsu)
     * @변경이력 :
     * @Method 설명 : 건강검진내역 API 호출
     */
    public String getHealthCheckInfo(byte[] aesCipherKey, String postURL, String certFilePath, String keyFilePath, String txtCertPassword) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {

        byte[] _certCipherBytes = this._aes.Encrypt(Util.FileToByteArray(certFilePath));
        byte[] _keyCipherBytes = this._aes.Encrypt(Util.FileToByteArray(keyFilePath));
        byte[] _passwordCipherBytes = this._aes.Encrypt(new String(txtCertPassword).getBytes("US-ASCII"));

        HashMap<String, String> _bodyMap = new HashMap<String, String>();
        _bodyMap.put("CertFile", Util.base64Encode(_certCipherBytes));
        _bodyMap.put("KeyFile", Util.base64Encode(_keyCipherBytes));
        _bodyMap.put("CertPassword", Util.base64Encode(_passwordCipherBytes));

        RequestBody _body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new Gson().toJson(_bodyMap));
        OkHttpClient _client = new OkHttpClient()
                .newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS) // connect timeout
                .writeTimeout(30, TimeUnit.SECONDS) // write timeout
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request _request = new Request.Builder()
                .url(_apiHeader + postURL)
                .post(_body)
                .addHeader("Content-Type", "application/json")
                .addHeader("API-Key", this._apiKey)
                .addHeader("ENC-Key", Util.base64Encode(aesCipherKey))
                .build();

        Response _response = _client.newCall(_request).execute();
        String _responseStr = _response.body().string();

//        _logger.info("========== getMedicalCheckInfo REQUEST ==========");
//        _logger.info(" ");
//        _logger.info("API-Key:" + _apiKey);
//         _logger.info(" ");
//        _logger.info("ENC-Key:{}", Util.base64Encode(aesCipherKey));
//         _logger.info(" ");
//        _logger.info("CertFile:" + Util.base64Encode(_certCipherBytes));
//         _logger.info(" ");
//        _logger.info("KeyFile:" + Util.base64Encode(_keyCipherBytes));
//         _logger.info(" ");
//        _logger.info("CertPassword:" + Util.base64Encode(_passwordCipherBytes));

        return _responseStr;
    }

    /**
     * @param aesCipherKey    : ENC-Key
     * @param certFilePath    : 인증서 공개키
     * @param keyFilePath     : 인증서 개인키
     * @param txtCertPassword : 인증서 비밀번호
     * @Method Name : getTreatment
     * @작성일 : 2022. 1. 28.
     * @작성자 : HEM(yechanj, yejin, yxxnsu)
     * @변경이력 :
     * @Method 설명 : 진료 및 투약 정보
     */

    public String getMedicalTreatment(byte[] aesCipherKey, String postURL, String certFilePath, String keyFilePath, String txtCertPassword)
                    throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                    InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {

            byte[] _certCipherBytes = this._aes.Encrypt(Util.FileToByteArray(certFilePath));
            byte[] _keyCipherBytes = this._aes.Encrypt(Util.FileToByteArray(keyFilePath));
            byte[] _passwordCipherBytes = this._aes.Encrypt(new String(txtCertPassword).getBytes("US-ASCII"));

            HashMap<String, String> _bodyMap = new HashMap<String, String>();
            _bodyMap.put("CertFile", Util.base64Encode(_certCipherBytes));
            _bodyMap.put("KeyFile", Util.base64Encode(_keyCipherBytes));
            _bodyMap.put("CertPassword", Util.base64Encode(_passwordCipherBytes));

            RequestBody _body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                            new Gson().toJson(_bodyMap));
            OkHttpClient _client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

            Request _request = new Request.Builder().url(_apiHeader + postURL).post(_body)
                            .addHeader("Content-Type", "application/json").addHeader("API-Key", this._apiKey)
                            .addHeader("ENC-Key", Util.base64Encode(aesCipherKey)).build();

            Response _response = _client.newCall(_request).execute();
            String _responseStr = _response.body().string();

        //     _logger.info("========== getCertRegister REQUEST ==========");
        //     _logger.info(" ");
        //     _logger.info("API-Key:" + _apiKey);
        //     _logger.info(" ");
        //     _logger.info("ENC-Key:{}", Util.base64Encode(aesCipherKey));
        //     _logger.info(" ");
        //     _logger.info("CertFile:" + Util.base64Encode(_certCipherBytes));
        //     _logger.info(" ");
        //     _logger.info("KeyFile:" + Util.base64Encode(_keyCipherBytes));
        //     _logger.info(" ");
        //     _logger.info("CertPassword:" + Util.base64Encode(_passwordCipherBytes));

            return _responseStr;
    }
     

    /**
     * @param aesCipherKey      : ENC-Key
     * @param certFilePath      : 인증서 공개키
     * @param keyFilePath       : 인증서 개인키
     * @param txtIdentityNumber : 주민등록번호
     * @param txtCertPassword   : 인증서 비밀번호
     * @return : 결과
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     * @Method Name : getCertRegister
     * @작성일 : 2022. 2. 16.
     * @작성자 : HEM(yechanj, yejin, yxxnsu)
     * @변경이력 :
     * @Method 설명 : 인증서등록 - 건강보험공단 인증서 등록 API
     */
    public String getCertRegister(byte[] aesCipherKey, String postURL, String certFilePath, String keyFilePath, String txtIdentityNumber,
                    String txtCertPassword)
                    throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                    InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {

            byte[] _certCipherBytes = this._aes.Encrypt(Util.FileToByteArray(certFilePath));
            byte[] _keyCipherBytes = this._aes.Encrypt(Util.FileToByteArray(keyFilePath));
            byte[] _identityCipherBytes = this._aes.Encrypt(txtIdentityNumber.replace("-", "").getBytes("UTF-8"));
            byte[] _passwordCipherBytes = this._aes.Encrypt(new String(txtCertPassword).getBytes("UTF-8"));

            _logger.info("========= MY CERT REGISTER REQUEST =========");
            _logger.info(" ");

            HashMap<String, String> _bodyMap = new HashMap<String, String>();
            _bodyMap.put("CertFile", Util.base64Encode(_certCipherBytes));
            _bodyMap.put("KeyFile", Util.base64Encode(_keyCipherBytes));
            _bodyMap.put("IdentityNumber", Util.base64Encode(_identityCipherBytes));
            _bodyMap.put("CertPassword", Util.base64Encode(_passwordCipherBytes));

            RequestBody _body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                            new Gson().toJson(_bodyMap));

            OkHttpClient _client = new OkHttpClient().newBuilder().build();
            Request _request = new Request.Builder()
                            .url(_apiHeader + postURL)
                            .post(_body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("API-Key", this._apiKey)
                            .addHeader("ENC-Key", Util.base64Encode(aesCipherKey))
                            .build();

            Response _response = _client.newCall(_request).execute();
            String _responseStr = _response.body().string();

            _logger.info("response:" + _response.toString());
            _logger.info("responseStr:" + _responseStr.toString());

            _logger.info("========= MY_Drug REQUEST =========");
            _logger.info(" ");
            _logger.info("Response :" + _responseStr);
            _logger.info("API-Key:" + _apiKey);
            _logger.info(" ");
            _logger.info("ENC-Key:{}", Util.base64Encode(aesCipherKey));
            _logger.info(" ");
            _logger.info("CertFile:" + Util.base64Encode(_certCipherBytes));
            _logger.info(" ");
            _logger.info("KeyFile:" + Util.base64Encode(_keyCipherBytes));
            _logger.info(" ");
            _logger.info("IdentityNumber:" + Util.base64Encode(_identityCipherBytes));
            _logger.info(" ");
            _logger.info("CertPassword:" + Util.base64Encode(_passwordCipherBytes));
            _logger.info(" ");

            return _responseStr;
    }



}