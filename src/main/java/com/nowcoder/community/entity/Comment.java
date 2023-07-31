package com.nowcoder.community.entity;

import java.util.Date;
import java.util.Objects;

public class Comment {

    private int id;
    private int userId;

    // 对什么类型的实体进行评论
    // 1：对帖子进行的评论，2：对评论进行的评论（也就是回复）
    private int entityType;

    // 目标实体的id
    // 若entityType为帖子，则entityId为DiscussPost的id
    // 若entityType为评论，则entityId为Comment的id
    private int entityId;

    // 对某个人的评论进行评论（回复）时，targetId为   那个人的id
    // entityType为帖子时，没有targetId。
    private int targetId;
    private String content;
    private int status;
    private Date createTime;
    

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEntityType() {
        return entityType;
    }

    public void setEntityType(int entityType) {
        this.entityType = entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", userId=" + userId +
                ", entityType=" + entityType +
                ", entityId=" + entityId +
                ", targetId=" + targetId +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
    
}
