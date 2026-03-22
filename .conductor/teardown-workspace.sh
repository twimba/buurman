#!/bin/bash
set -euo pipefail

echo "Starting workspace teardown..."

# Run the teardown script
make workspace-teardown

echo ""
echo "Workspace torn down. This worktree is ready for cleanup or reuse."
