# Maintaining Goshawk alongside Sparrow

## Separation

`upstream` points to Sparrow; `origin` points to the public GitHub fork
`linzhm/Goshawk`, whose parent is `sparrowwallet/sparrow`. The native fork
relationship shares upstream Git history on GitHub. Goshawk changes live on
`origin/main`; review upstream changes from `upstream/master` using the workflow
below before integrating them.
Keep original history and submodule pins. Do not rename Java packages, move
upstream source trees or rewrite wallet formats merely to change branding.

| Location | Ownership |
| --- | --- |
| `src/main/`, `drongo/`, `lark/` | Upstream code, with a small integration patch |
| `branding/goshawk/src/` | Goshawk product constants |
| `branding/goshawk/resources/` | Goshawk resource overrides and new images |
| `branding/goshawk/deploy/` | Goshawk installer metadata and icons |
| `branding/goshawk/brand.gradle` | Overlay rules and notice preservation |
| `scripts/brand/`, `docs/brand/` | Editable visual master and exported identity |

Resource overrides use the same classpath path as the upstream file. The build
excludes the upstream copy only if an explicit override exists, then fails on
any other duplicate. This preserves upstream sources and deterministic output.
An overlay is still a copy: upstream changes to an overridden FXML/CSS file
must be reviewed and incorporated. `upstream-sync.sh preview` highlights those
paths instead of silently treating the brand layer as permanently compatible.

## Choose the synchronization operation

- **Merge** when accepting the complete upstream history to a selected commit.
  It preserves ancestry and makes subsequent merges easier.
- **Cherry-pick** when adopting selected, self-contained commits. Preserve the
  upstream SHA in the resulting commit (`-x`). Include prerequisite commits.
- **Partial patch** when only selected paths or hunks are appropriate. Stage
  only those hunks, describe omissions, and record `Upstream-Commit: <sha>`.
  This is not a merge of the entire upstream commit.

Never use `merge -s ours`, blanket `-X ours`, or a custom “always keep ours”
driver to hide conflicts in transaction, signing or wallet code. Such a merge
can record rejected upstream changes as integrated and suppress future updates.

## Workflow

```sh
./scripts/upstream-sync.sh preview upstream/master
./scripts/upstream-sync.sh prepare upstream/master
# Or pick specific upstream commits, in dependency order:
./scripts/upstream-sync.sh prepare upstream/master <sha1> <sha2>
```

The script fetches only `upstream`, produces a change/overlay report, and
prepares an isolated worktree and branch. It does not publish, update the main
branch, resolve conflicts or commit a whole merge automatically. On failure it
leaves the worktree intact for inspection. A selected-commit run uses `-x`
to retain provenance. Uncommitted changes in the current checkout cause the
preparation step to stop.

For partial changes, start from a dedicated branch, use `git cherry-pick -n`
for a candidate commit, discard unwanted paths/hunks deliberately, and commit
the selected change with its source SHA and omissions. A partial import is
necessarily a reviewed operation; the script does not guess which hunks matter.

Git normally keeps independent edits automatically. `git config rerere.enabled
true` can reuse previous conflict resolutions; leave `rerere.autoupdate false`
so reused resolutions still require review before staging. This remembers text
resolutions, not whether the resulting code is correct for BSV.

## Review and validation

Inspect all overridden resource paths in the report, the small identity hooks,
and any build/dependency changes. Treat transaction serialization, sighash,
script rules, address derivation, fee policy, mempool/replacement behavior,
headers/proofs and wallet persistence as BSV-sensitive changes. They require
semantic review even if Git reports no conflicts.

Run the applicable tests, `./gradlew classes`, inspect the packaged About and
welcome surfaces, and check the generated dependency notice inventory. Update
`MODIFICATIONS.md`, the baseline record and any changed-file notices before
committing and integrating the prepared branch. Do not update Drongo or Lark
independently without checking the matching Sparrow changes.
