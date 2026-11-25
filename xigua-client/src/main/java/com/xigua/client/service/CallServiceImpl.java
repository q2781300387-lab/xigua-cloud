package com.xigua.client.service;

import com.xigua.api.service.CallService;
import com.xigua.common.sequence.sequence.Sequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName CallServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/13 21:26
 */
@Service
public class CallServiceImpl implements CallService {
    @Autowired
    private Sequence sequence;

    @Override
    public String createCallId() {
        return sequence.nextNo();
    }
}
