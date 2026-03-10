package com.github.catatafishen.ideagentforcopilot.psi;

import com.intellij.openapi.project.Project;

/**
 * Git tool handlers — thin wrappers delegating to {@link GitCommands}.
 */
class GitTools extends AbstractToolHandler {

    GitTools(Project project, GitToolHandler gitToolHandler) {
        super(project);
        var commands = new GitCommands(gitToolHandler);
        register("git_status", commands::gitStatus);
        register("git_diff", commands::gitDiff);
        register("git_log", commands::gitLog);
        register("git_blame", commands::gitBlame);
        register("git_commit", commands::gitCommit);
        register("git_stage", commands::gitStage);
        register("git_unstage", commands::gitUnstage);
        register("git_branch", commands::gitBranch);
        register("git_stash", commands::gitStash);
        register("git_revert", commands::gitRevert);
        register("git_show", commands::gitShow);
        register("git_push", commands::gitPush);
        register("git_remote", commands::gitRemote);
        register("git_fetch", commands::gitFetch);
        register("git_pull", commands::gitPull);
        register("git_merge", commands::gitMerge);
        register("git_rebase", commands::gitRebase);
        register("git_cherry_pick", commands::gitCherryPick);
        register("git_tag", commands::gitTag);
        register("git_reset", commands::gitReset);
        register("get_file_history", commands::getFileHistory);
    }
}
