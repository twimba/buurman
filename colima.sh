#!/opt/homebrew/bin/fish
set -x TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE /var/run/docker.sock
set -x TESTCONTAINERS_HOST_OVERRIDE (colima ls -j | jq -r '.address')
set -x DOCKER_HOST "unix://$HOME/.colima/default/docker.sock"
set -x TESTCONTAINERS_RYUK_DISABLED true
