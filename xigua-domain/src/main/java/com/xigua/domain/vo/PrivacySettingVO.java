package com.xigua.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PrivacySettingVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer notificationPreview = 1;
    private Integer notificationSound = 1;
    private Integer showOnlineStatus = 1;
    private Integer showReadReceipt = 1;
    private Integer allowPhoneSearch = 1;
    private Integer friendVerify = 1;
}
