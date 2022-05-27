package com.swfact.tilko_plugin.model;

import com.google.gson.Gson;

/**
 * @FileName : RsaPublicKey.java
 * @Project : TilkoSampleSource
 * @Date : 2020. 8. 13.
 * @작성자 : Tilko.net
 * @변경이력 :
 * @프로그램 설명 : RSA 공개키 요청 결과 데이터 모델
 */
public class RsaPublicKey extends BaseModel {
    /**
     * API 키에 매칭되는 RSA 공개키
     */
    private String PublicKey;	//사용안함

    /**
     * 전달한 API 키(검증 용)
     */
    private String ApiKey;



    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RsaPublicKey [getPublicKey()=").append(getPublicKey()).append(", getApiKey()=")
                .append(getApiKey()).append(", getModulus()=").append(getStatus()).append(", getMessage()=")
                .append(getMessage()).append("]");
        return builder.toString();
    }

    public final String getPublicKey() {
        return PublicKey;
    }

    public final void setPublicKey(String value) {
        PublicKey = value;
    }

    public final String getApiKey() {
        return ApiKey;
    }

    public final void setApiKey(String value) {
        ApiKey = value;
    }

    public static void main(String[] args) {
        String jsonStrSuccess = "{\"Status\":\"OK\",\"Message\":\"성공\",\"PublicKey\":\"BgIAAACkAABSU0ExAAQAAAEAAQChOBmrNkicAzaPMLZyC8akxMgYqh3kSnjGDnP9ubhQemOfKwnlakVTjf/v3P0IqkF2HRtJuNGPLP2oXvv5Sr3z84XgeOIE66/NxrkmT3bbD+W47YDsuL3m6yF5BCNefhte882FcTAXhAhnZApAk6Io3MJzqF4vWoG/fqVXs6ITrA==\",\"Modulus\":\"rBOis1elfr+BWi9eqHPC3Ciik0AKZGcIhBcwcYXN814bfl4jBHkh6+a9uOyA7bjlD9t2Tya5xs2v6wTieOCF8/O9Svn7Xqj9LI/RuEkbHXZBqgj93O//jVNFauUJK59jelC4uf1zDsZ4SuQdqhjIxKTGC3K2MI82A5xINqsZOKE=\",\"Exponent\":\"AQAB\",\"ApiKey\":\"931ad30aabca4e57b9219a4f1a3b90f2\"}";
        String jsonStrFail = "{\"Status\":\"Error\",\"Message\":\"API-Key의 값이 없습니다.\",\"PublicKey\":\"\",\"ApiKey\":\"111\"}";
        RsaPublicKey jsonObj = (RsaPublicKey) new Gson().fromJson(jsonStrSuccess, RsaPublicKey.class);
        System.out.println(jsonObj);
        jsonObj = (RsaPublicKey) new Gson().fromJson(jsonStrFail, RsaPublicKey.class);
        System.out.println(jsonObj);

    }


}