package com.dabsquared.gitlabjenkins.listener;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Extension;
import hudson.Util;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabMergeRequestRunListener extends RunListener<Run<?, ?>> {
    private static final Logger LOGGER = Logger.getLogger(GitLabMergeRequestRunListener.class.getName());

    @Override
    public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
        GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(build.getParent());
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);

        if (trigger != null && cause != null && cause.getData().getActionType() == CauseData.ActionType.MERGE) {
            Result buildResult = build.getResult();
            Integer projectId = cause.getData().getProjectId();
            Integer mergeRequestId = cause.getData().getMergeRequestId();
            if (buildResult == Result.SUCCESS) {
                acceptMergeRequestIfNecessary(build, trigger, listener, projectId.toString(), mergeRequestId);
            }
            addNoteOnMergeRequestIfNecessary(build, trigger, listener, projectId.toString(), mergeRequestId);
        }
    }

    private String getBuildUrl(Run<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

    private void acceptMergeRequestIfNecessary(Run<?, ?> build, GitLabPushTrigger trigger, TaskListener listener, String projectId, Integer mergeRequestId) {
        if (trigger.getAcceptMergeRequestOnSuccess()) {
            try {
                GitLabApi client = getClient(build);
                if (client == null) {
                    listener.getLogger().println("No GitLab connection configured");
                } else {
                    client.acceptMergeRequest(projectId, mergeRequestId, "Merge Request accepted by jenkins build success", false);
                }
            } catch (WebApplicationException e) {
                listener.getLogger().println("Failed to accept merge request.");
            }
        }
    }

    private void addNoteOnMergeRequestIfNecessary(Run<?, ?> build, GitLabPushTrigger trigger, TaskListener listener, String projectId, Integer mergeRequestId) {
        if (trigger.getAddNoteOnMergeRequest()) {
            try {
                GitLabApi client = getClient(build);
                if (client == null) {
                    listener.getLogger().println("No GitLab connection configured");
                } else {
                    StringBuilder message = getNote(build, trigger, listener);
                    client.createMergeRequestNote(projectId, mergeRequestId, message.toString());
                }
            } catch (WebApplicationException e) {
                listener.getLogger().println("Failed to add message to merge request.");
            }
        }
    }

    private String getResultIcon(GitLabPushTrigger trigger, Result result) {
        if (result == Result.SUCCESS) {
            return trigger.getAddVoteOnMergeRequest() ? ":+1:" : ":white_check_mark:";
        } else if (result == Result.ABORTED) {
            return ":point_up:";
        } else {
            return trigger.getAddVoteOnMergeRequest() ? ":-1:" : ":anguished:";
        }
    }

    private GitLabApi getClient(Run<?, ?> run) {
        GitLabConnectionProperty connectionProperty = run.getParent().getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getClient();
        }
        return null;
    }

    public static String replaceMacros(Run<?, ?> build, TaskListener listener, String inputString) {
        String returnString = inputString;
        if (build != null && inputString != null) {
            try {
                Map<String, String> messageEnvVars = getEnvVars(build, listener);
                returnString = Util.replaceMacro(inputString, messageEnvVars);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Couldn't replace macros in message: ", e);
            }
        }
        return returnString;
    }

    public static Map<String, String> getEnvVars(Run<?, ?> build, TaskListener listener) {
        Map<String, String> messageEnvVars = new HashMap<String, String>();
        if (build != null) {
            messageEnvVars.putAll(build.getCharacteristicEnvVars());
//            messageEnvVars.putAll(build.getBuildVariables());
            try {
                messageEnvVars.putAll(build.getEnvironment(listener));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Couldn't get Env Variables: ", e);
            }
        }
        return messageEnvVars;
    }

    public StringBuilder getNote(Run<?, ?> build, GitLabPushTrigger trigger, TaskListener listener) {
        StringBuilder msg = new StringBuilder();
        String message;
        String icon = getResultIcon(trigger, build.getResult());
        String buildUrl = Jenkins.getInstance().getRootUrl() + build.getUrl();
        String defaultNote = MessageFormat.format("{0} Jenkins Build {1}\n\nResults available at: [Jenkins [{2} #{3}]]({4})",
            getResultIcon(trigger, build.getResult()), build.getResult().color.getDescription(), build.getParent().getDisplayName(), build.getNumber(), buildUrl);

        if (trigger.getNotesCustomize()) {
            if (build.getResult() == Result.SUCCESS) {
                message = replaceMacros(build, listener, trigger.getSuccessNoteOnMergeRequests());
            } else if (build.getResult() == Result.ABORTED) {
                message = replaceMacros(build, listener, trigger.getAbortNoteOnMergeRequests());
            } else {
                message = replaceMacros(build, listener, trigger.getFailureNoteOnMergeRequests());
            }
            msg.append(message);
        } else {
            msg.append(icon).append(defaultNote);
        }

        return msg;
    }
}
