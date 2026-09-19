/**
 * Run master tests unless this suite passed for the merged PR's final revision.
 * Missing evidence or API failures always fall back to running the tests.
 */
module.exports = async function shouldRun({ github, context, core, workflowId }) {
  try {
    const pullRequests = await github.paginate(
      github.rest.repos.listPullRequestsAssociatedWithCommit,
      { ...context.repo, commit_sha: context.sha, per_page: 100 },
    );
    const mergedPr = pullRequests.find(pr =>
      pr.merged_at && pr.base.ref === 'master' && pr.merge_commit_sha === context.sha,
    );
    if (!mergedPr) return true;

    const runs = await github.paginate(github.rest.actions.listWorkflowRuns, {
      ...context.repo,
      workflow_id: workflowId,
      event: 'pull_request',
      head_sha: mergedPr.head.sha,
      status: 'success',
      per_page: 100,
    });
    for (const run of runs) {
      // Require evidence that the successful run belongs to this PR and base.
      if (!run.pull_requests.some(pr => pr.number === mergedPr.number && pr.base.ref === 'master')) {
        continue;
      }
      const jobs = await github.paginate(github.rest.actions.listJobsForWorkflowRun, {
        ...context.repo, run_id: run.id, filter: 'latest', per_page: 100,
      });
      // A draft PR can have a successful workflow with a skipped test job.
      if (jobs.some(job => job.name === 'test' && job.conclusion === 'success')) {
        core.info(`Skipping ${workflowId}: PR #${mergedPr.number} passed in run ${run.id}.`);
        return false;
      }
    }
    return true;
  } catch (error) {
    core.warning(`Unable to verify prior PR tests; running ${workflowId}: ${error.message}`);
    return true;
  }
};
