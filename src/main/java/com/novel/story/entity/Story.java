package com.novel.story.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("stories")
public class Story {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String idea;
    private String styleGuide;
    private String proposal;
    @TableField("outline_text")
    private String outline;
    private String body;
    private String stage;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIdea() { return idea; }
    public void setIdea(String idea) { this.idea = idea; }
    public String getStyleGuide() { return styleGuide; }
    public void setStyleGuide(String styleGuide) { this.styleGuide = styleGuide; }
    public String getProposal() { return proposal; }
    public void setProposal(String proposal) { this.proposal = proposal; }
    public String getOutline() { return outline; }
    public void setOutline(String outline) { this.outline = outline; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
