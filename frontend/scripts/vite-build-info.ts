import { execSync } from 'child_process'

function gitExec(cmd: string): string {
  try {
    return execSync(cmd).toString().trim()
  } catch {
    return 'unknown'
  }
}

export function getGitInfo() {
  const env = process.env
  return {
    gitCommit: env.GIT_COMMIT_SHORT || gitExec('git rev-parse --short HEAD'),
    gitCommitFull: env.GIT_COMMIT || gitExec('git rev-parse HEAD'),
    gitBranch: env.GIT_BRANCH || gitExec('git rev-parse --abbrev-ref HEAD'),
  }
}

export function getBuildDefines(version: string) {
  const { gitCommit, gitCommitFull, gitBranch } = getGitInfo()
  const buildTime = new Date().toISOString()
  return {
    defines: {
      __APP_VERSION__: JSON.stringify(version),
      __GIT_COMMIT__: JSON.stringify(gitCommit),
      __GIT_COMMIT_FULL__: JSON.stringify(gitCommitFull),
      __GIT_BRANCH__: JSON.stringify(gitBranch),
      __BUILD_TIME__: JSON.stringify(buildTime),
    },
    raw: { version, gitCommit, gitCommitFull, gitBranch, buildTime },
  }
}
