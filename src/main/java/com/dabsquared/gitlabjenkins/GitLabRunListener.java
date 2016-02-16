package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.triggers.Trigger;
import jenkins.model.ParameterizedJobMixIn;

import javax.annotation.Nonnull;

/**
 * RunListener that will be called when a build starts and completes.
 * Will lookup GitLabPushTrigger and call onStarted and onCompleted methods
 * in order to have access to the build and set properties.
 */
@Extension
public class GitLabRunListener extends RunListener<AbstractBuild<?, ?>> {

    @Override
    public void onCompleted(AbstractBuild<?, ?> run, @Nonnull TaskListener listener) {
        GitLabPushTrigger trig = getTrigger(run);
        if (trig != null) {
            trig.onCompleted(run, listener);
        }
        super.onCompleted(run, listener);
    }

    @Override
    public void onStarted(AbstractBuild<?, ?> run, TaskListener listener) {
        GitLabPushTrigger trig = getTrigger(run);
        if (trig != null) {
            trig.onStarted(run);
        }
        super.onStarted(run, listener);
    }


    private GitLabPushTrigger getTrigger(AbstractBuild<?, ?> run) {
        if (run instanceof AbstractBuild) {
            ParameterizedJobMixIn.ParameterizedJob p = ((AbstractBuild) run).getProject();
            for (Trigger t : p.getTriggers().values()) {
                if (t instanceof GitLabPushTrigger)
                    return (GitLabPushTrigger) t;
            }
        }

        return null;
    }
}
