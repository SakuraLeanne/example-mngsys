package com.dhgx.api.notify.core;

import java.util.Map;

@FunctionalInterface
public interface EventNotifyHandler {
    /**
     * 处理消费到的消息。
     *
     * @param messageId Redis Stream 生成的消息 ID
     * @param body      消息体，使用字段键值对承载业务数据
     */
    void onMessage(String messageId, Map<String, String> body);
}
