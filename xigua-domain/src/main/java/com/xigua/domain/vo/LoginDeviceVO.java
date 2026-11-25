package com.xigua.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class LoginDeviceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String userAgent;
    private String platform;
    private String ip;
    private String region;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    private Boolean current;
}
