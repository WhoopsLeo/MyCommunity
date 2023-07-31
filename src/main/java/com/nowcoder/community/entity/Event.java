package com.nowcoder.community.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {

    // 事件的主题
    private String topic;
    // 事件的发起人，代表着系统消息的发起人。
    private int userId;
    // 事件发生在某个实体上，那个实体类型
    private int entityType;
    // 事件发生在某个实体上，那个实体的id
    private int entityId;
    // 事件发生在某个实体上，那个实体的作者。代表着系统消息要发给谁。
    private int entityUserId;
    // 所有其他额外的数据存在map里，使实体具有扩展性。比如：帖子的id。
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    // return this;的作用：在new出对象后，可以连续用几个set方法来存入数据。a.set().set();
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
