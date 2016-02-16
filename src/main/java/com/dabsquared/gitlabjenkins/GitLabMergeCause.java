package com.dabsquared.gitlabjenkins;

import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;

/**
 * Created by daniel on 6/8/14.
 */
public class GitLabMergeCause extends SCMTrigger.SCMTriggerCause {

    private GitLabMergeRequest mergeRequest;

    public GitLabMergeCause(GitLabMergeRequest mergeRequest) {
        this.mergeRequest = mergeRequest;
    }

    public GitLabMergeCause(GitLabMergeRequest mergeRequest, File logFile) throws IOException {
        super(logFile);
        this.mergeRequest = mergeRequest;
    }

    public GitLabMergeCause(GitLabMergeRequest mergeRequest, String pollingLog) {
        super(pollingLog);
        this.mergeRequest = mergeRequest;
    }

    public GitLabMergeRequest getMergeRequest() {
        return mergeRequest;
    }

    @Override
    public String getShortDescription() {
	Integer iid = this.mergeRequest.getObjectAttribute().getIid();
	String sourceBranch = this.mergeRequest.getObjectAttribute().getSourceBranch();
	String targetBranch = this.mergeRequest.getObjectAttribute().getTargetBranch();

        return "GitLab Merge Request #" + iid + " : " + sourceBranch + " => " + targetBranch;
    }

}
