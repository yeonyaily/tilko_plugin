package com.swfact.tilko_plugin.model;

/**
 * @FileName : AuthResponse.java
 * @Project : TilkoSampleSource
 * @Date : 2020. 8. 13.
 * @작성자 : Tilko.net
 * @변경이력 :
 * @프로그램 설명 : 건강보험공단 인증결과 데이터 모델
 */
public class AuthResponse extends BaseModel {
    private String AuthCode;

    public final String getAuthCode() {
        return AuthCode;
    }

    public final void setAuthCode(String value) {
        AuthCode = value;
    }
}