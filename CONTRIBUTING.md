# Contributing

Thanks for your interest in improving OmniFaces!

## Branch model

Each major version has its own maintenance branch.

| Branch | Minimum Java | Minimum Faces |
|---|---|---|
| `5.x` | 17 | Faces 4.1 |
| `4.x` | 11 | Faces 3.0 |
| `3.x` | 8 | JSF 2.3 |

The `2.x` and `1.x` branches receive security fixes only.

`5.x` is the default branch.
New features go into `5.x` only.
A bug fix targets the oldest branch it applies to.
Maintainers merge each branch into the next one up after a change lands, so a fix on `3.x` also reaches `4.x` and `5.x`.
If you are unsure which branch your fix belongs on, target `5.x` and say so in the pull request.

## Submitting changes

1. Fork the repository.
2. Create a branch off the target branch for your change, e.g. `fix/some-bug`.
3. Make your changes.
   Keep the scope focused: one logical change per pull request.
4. Add or update integration tests for the changed behavior.
   See `DEVELOPERS.md` on the target branch for how to build and run the tests.
5. Open a pull request against the target branch.
6. Describe what changes and *why*.
   Link related issues, e.g. `Closes #123`.

Keep reformatting and renaming out of a pull request with substantive changes.
Open a separate pull request for those.

## AI-assisted contributions

AI coding assistants are welcome.
The same rules apply as to any other contribution, plus these:

- **You are the author.**
  You must understand every line you submit, and be able to explain and defend it in review.
  "The AI wrote it" is not an answer to a review comment.
- **Disclose it.**
  State in the pull request description which tool and model you used, and for what, e.g. "Claude Code with Claude Opus 5.5 wrote the test, I wrote the fix".
  Always include the model name and version, as your tool reports it, because model versions behave differently.
  A `Co-Authored-By` trailer in the commit message counts as disclosure when it names the model and version, e.g. `Co-Authored-By: Claude Opus 5.5`.
- **Verify it.**
  Build and run the tests locally before opening the pull request.
  Never claim a behavior, a default or a spec rule you have not checked.

Pull requests that ignore these rules may be closed without review.

## Releases

Contributors don't need to touch versions; leave that to the release.
Maintainers release all affected branches together, oldest first.
Per branch:

1. Set the project `<version>` in `pom.xml` to `X.Y.Z`, commit it as `Prepare X.Y.Z release`, and deploy to Maven Central with `mvn clean deploy -P release`.
2. Tag the release commit `X.Y.Z`: lightweight, no `v` prefix.
   Push the branch and the tag together: `git push origin <branch> X.Y.Z`.
3. Set the version to the next patch `-SNAPSHOT`, commit it as `Branch is now ready for X.Y.Z development`, and push.
4. Merge the branch into the next one up.

After all branches are released, update the download page and documentation in [omnifaces.github.io](https://github.com/omnifaces/omnifaces.github.io), and the version and "What's new" entries in [showcase](https://github.com/omnifaces/showcase).
