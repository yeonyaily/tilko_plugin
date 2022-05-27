package com.swfact.tilko_plugin.model;

/**
 * @FileName : BaseModel.java
 * @Project : TilkoSampleSource
 * @Date : 2020. 8. 13.
 * @작성자 : Tilko.net
 * @변경이력 :
 * @프로그램 설명 : 공통 반환 모델
 */
public class BaseModel {
    private String Status;
    private String Message;

    public final String getStatus() {
        return Status;
    }

    public final void setStatus(String value) {
        Status = value;
    }

    public final String getMessage() {
        return Message;
    }

    public final void setMessage(String value) {
        Message = value;
    }
}