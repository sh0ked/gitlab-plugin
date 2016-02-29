package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.data.Note;
import com.dabsquared.gitlabjenkins.data.MergeRequest;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabCommitStatus;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;

public class GitLabNoteRequest extends GitLabRequest {

	public static GitLabNoteRequest create(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload should not be null");
        }

        GitLabNoteRequest noteRequest =  Builder.INSTANCE.get().fromJson(payload, GitLabNoteRequest.class);
        return noteRequest;
    }

    public GitLabNoteRequest() {
    }

    private String object_kind;

    private Note objectAttributes;

    private MergeRequest mergeRequest;

    private GitlabProject sourceProject = null;

    public GitlabProject getSourceProject (GitLab api) throws IOException {
        if (sourceProject == null) {
            sourceProject = api.instance().getProject(mergeRequest.getSourceProjectId());
        }
        return sourceProject;
    }

    public String getObject_kind() {
        return object_kind;
    }

    public void setObject_kind(String objectKind) {
        this.object_kind = objectKind;
    }

    public Note getObjectAttribute() {
        return objectAttributes;
    }

    public void setObjectAttribute(Note objectAttributes) {
        this.objectAttributes = objectAttributes;
    }

    public MergeRequest getMergeRequestAttribute() {
        return mergeRequest;
    }

    public void setMergeRequestAttribute(MergeRequest mergeRequest) {
        this.mergeRequest = this.mergeRequest;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public GitlabCommitStatus createCommitStatus(GitlabAPI api, String status, String targetUrl) {
        try {
            if (mergeRequest.getLastCommit() != null) {
                return api.createCommitStatus(sourceProject, mergeRequest.getLastCommit().getId(), status, mergeRequest.getLastCommit().getId(), "Jenkins", targetUrl, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
