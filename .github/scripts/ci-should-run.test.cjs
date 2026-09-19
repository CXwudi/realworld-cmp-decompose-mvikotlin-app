const assert = require('node:assert/strict');
const test = require('node:test');
const shouldRun = require('./ci-should-run.cjs');

/** Supply GitHub responses without making network requests. */
function fixture({ prs, runs, jobs, error } = {}) {
  const calls = [];
  const warnings = [];
  const mergedPr = {
    number: 42, merged_at: '2026-09-19T12:00:00Z', merge_commit_sha: 'master-tip',
    base: { ref: 'master' }, head: { sha: 'pr-head' },
  };
  const context = { repo: { owner: 'example', repo: 'conduit' }, sha: 'master-tip' };
  const github = {
    rest: {
      repos: { listPullRequestsAssociatedWithCommit: 'prs' },
      actions: { listWorkflowRuns: 'runs', listJobsForWorkflowRun: 'jobs' },
    },
    paginate: async (method, params) => {
      calls.push({ method, params });
      if (error) throw error;
      if (method === 'prs') return prs ?? [mergedPr];
      if (method === 'runs') return runs ?? [{ id: 100, pull_requests: [mergedPr] }];
      if (method === 'jobs') return jobs ?? [{ name: 'test', conclusion: 'success' }];
      throw new Error(`Unexpected endpoint: ${method}`);
    },
  };
  return {
    args: {
      github, context, workflowId: 'frontend-logic-test.yml',
      core: { info: () => {}, warning: message => warnings.push(message) },
    },
    mergedPr, calls, warnings,
  };
}

test('a successfully tested PR skips master tests and queries the exact suite and head', async () => {
  const { args, calls } = fixture();
  assert.equal(await shouldRun(args), false);
  assert.deepEqual(calls[0].params, {
    owner: 'example', repo: 'conduit', commit_sha: 'master-tip', per_page: 100,
  });
  assert.deepEqual(calls[1].params, {
    owner: 'example', repo: 'conduit', workflow_id: 'frontend-logic-test.yml',
    event: 'pull_request', head_sha: 'pr-head', status: 'success', per_page: 100,
  });
  assert.equal(calls[2].params.filter, 'latest');
});

test('direct commits run tests without looking for workflow runs', async () => {
  const { args, calls } = fixture({ prs: [] });
  assert.equal(await shouldRun(args), true);
  assert.equal(calls.length, 1);
});

for (const [label, changes] of [
  ['an open PR', { merged_at: null }],
  ['a PR merged to another branch', { base: { ref: 'release' } }],
  ['a push containing a later direct commit', { merge_commit_sha: 'earlier-merge' }],
]) {
  test(`${label} cannot suppress master tests`, async () => {
    const { mergedPr } = fixture();
    const { args } = fixture({ prs: [{ ...mergedPr, ...changes }] });
    assert.equal(await shouldRun(args), true);
  });
}

test('no successful run for the final PR head means master tests run', async () => {
  const { args } = fixture({ runs: [] });
  assert.equal(await shouldRun(args), true);
});

for (const conclusion of ['skipped', 'failure', 'cancelled', null]) {
  test(`a ${conclusion} test job cannot suppress master tests`, async () => {
    const { args } = fixture({ jobs: [{ name: 'test', conclusion }] });
    assert.equal(await shouldRun(args), true);
  });
}

test('success of a different job is insufficient', async () => {
  const { args } = fixture({ jobs: [{ name: 'check-master', conclusion: 'success' }] });
  assert.equal(await shouldRun(args), true);
});

for (const pullRequests of [[], [{ number: 99, base: { ref: 'master' } }],
  [{ number: 42, base: { ref: 'release' } }]]) {
  test(`unrelated or missing PR association runs tests: ${JSON.stringify(pullRequests)}`, async () => {
    const { args } = fixture({ runs: [{ id: 100, pull_requests: pullRequests }] });
    assert.equal(await shouldRun(args), true);
  });
}

test('each suite checks its own workflow independently', async () => {
  const { args, calls } = fixture({ runs: [] });
  args.workflowId = 'http4k-backend-test.yml';
  assert.equal(await shouldRun(args), true);
  assert.equal(calls[1].params.workflow_id, 'http4k-backend-test.yml');
});

test('API errors fall back to running tests and report why', async () => {
  const { args, warnings } = fixture({ error: new Error('rate limited') });
  assert.equal(await shouldRun(args), true);
  assert.match(warnings[0], /rate limited/);
});
