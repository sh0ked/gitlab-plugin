package com.dabsquared.gitlabjenkins.data;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.gitlab.api.models.GitlabUser;

import java.util.Date;


public class Note {

    private Integer id;

    private String note;

    private Integer projectId;

    private User author;

    private Date createdAt;

    private Date updatedAt;

    public Note() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setprojectId(Integer projectId) {
        this.projectId = projectId;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setAuthor(GitlabUser author) {
        this.author = new User();
        this.author.setId(author.getId());
        this.author.setEmail(author.getEmail());
        this.author.setName(author.getName());
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
