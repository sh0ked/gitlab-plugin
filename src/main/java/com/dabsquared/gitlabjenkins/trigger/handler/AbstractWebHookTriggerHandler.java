package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.*;
import hudson.plugins.git.RevisionParameterAction;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.eclipse.jgit.transport.URIish;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public abstract class AbstractWebHookTriggerHandler<H extends WebHook> implements WebHookTriggerHandler<H> {

    private static final Logger LOGGER = Logger.getLogger(AbstractWebHookTriggerHandler.class.getName());

    @Override
    public void handle(Job<?, ?> job, H hook, boolean ciSkip, boolean stopBuildWithSameBranch, BranchFilter branchFilter) {
        if (ciSkip && isCiSkip(hook)) {
            LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
            return;
        }

        String targetBranch = getTargetBranch(hook);
        if (branchFilter.isBranchAllowed(targetBranch)) {
            LOGGER.log(Level.INFO, "{0} triggered for {1}.", LoggerUtil.toArray(job.getFullName(), getTriggerType()));
            if (stopBuildWithSameBranch) {
                try {
                    stopBuildWithSameBranch(job, hook);
                } catch (Exception e) {
                    LOGGER.warning("Error during stopping build with same branch: \n" + e.toString());
                    e.printStackTrace();
                }
            }
            scheduleBuild(job, createActions(job, hook));
        } else {
            LOGGER.log(Level.INFO, "branch {0} is not allowed", targetBranch);
        }
    }

    public void stopBuildWithSameBranch(Job<?, ?> job, H hook) throws IOException, ServletException {
        String sourceBranch = retrieveCauseData(hook).getSourceBranch();
        Run lastBuild = BuildUtil.getBuildByBranch(job, sourceBranch);
        Queue queue = Jenkins.getInstance().getQueue();

        for (hudson.model.Queue.Item item : queue.getItems()) {
            LOGGER.info("Queue Item: " + item.getParams());
            List itemParams = Arrays.asList(item.getParams().split("\n"));
            if (!itemParams.isEmpty()) {
                String sourceBranchParam = null;
                for (Object itemParam : itemParams) {
                    String paramItem = itemParam.toString();
                    sourceBranchParam = paramItem.startsWith("gitlabSourceBranch") ? Arrays.asList(paramItem.split("=")).get(1) : sourceBranchParam;
                }
                LOGGER.info("Compare build on branch " + sourceBranchParam + " in Queue with new build on branch " + sourceBranch);
                if (sourceBranchParam != null && sourceBranch.contentEquals(sourceBranchParam)) {
                    LOGGER.info("Found build for branch " + sourceBranch + " in queue and will be aborted now.");
                    queue.cancel(item);
                }
            }
        }

        if (lastBuild != null && lastBuild.isBuilding()) {
            if (job instanceof AbstractProject) {
                LOGGER.info("Found running build #" + lastBuild.getId() + " for branch " + sourceBranch + " and will be stop now.");
                ((AbstractProject) job).getRootProject().getBuild(lastBuild.getId()).doStop();
            } else {
                LOGGER.warning("Build was found but he has't been stopped because inside problem.");
            }
        }
    }

    protected abstract String getTriggerType();

    protected abstract boolean isCiSkip(H hook);

    private Action[] createActions(Job<?, ?> job, H hook) {
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new CauseAction(new GitLabWebHookCause(retrieveCauseData(hook))));
        try {
            actions.add(createRevisionParameter(hook));
        } catch (NoRevisionToBuildException e) {
            LOGGER.log(Level.WARNING, "unknown handled situation, dont know what revision to build for req {0} for job {1}",
                    new Object[]{hook, (job != null ? job.getFullName() : null)});
        }
        return actions.toArray(new Action[actions.size()]);
    }

    protected abstract CauseData retrieveCauseData(H hook);

    protected abstract String getTargetBranch(H hook);

    protected abstract RevisionParameterAction createRevisionParameter(H hook) throws NoRevisionToBuildException;

    protected URIish retrieveUrIish(WebHook hook) {
        try {
            if (hook.getRepository() != null) {
                return new URIish(hook.getRepository().getUrl());
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }
        return null;
    }

    private void scheduleBuild(Job<?, ?> job, Action[] actions) {
        int projectBuildDelay = 0;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob abstractProject = (ParameterizedJobMixIn.ParameterizedJob) job;
            if (abstractProject.getQuietPeriod() > projectBuildDelay) {
                projectBuildDelay = abstractProject.getQuietPeriod();
            }
        }
        retrieveScheduleJob(job).scheduleBuild2(projectBuildDelay, actions);
    }

    private ParameterizedJobMixIn retrieveScheduleJob(final Job<?, ?> job) {
        // TODO 1.621+ use standard method
        return new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };
    }
}
