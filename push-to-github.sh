#!/data/data/com.termux/files/usr/bin/bash
# Pushes this project to your GitHub repository from Termux.
# Usage: bash push-to-github.sh https://github.com/USER/lia-ai.git
set -e

REPO_URL="${1:-}"
if [ -z "$REPO_URL" ]; then
  echo "Usage: bash push-to-github.sh https://github.com/USER/lia-ai.git"
  exit 1
fi

pkg install -y git >/dev/null 2>&1 || true

cd "$(dirname "$0")"

if [ ! -d .git ]; then
  git init -b main
fi

git config user.name  "${GIT_USER_NAME:-Lia Developer}"
git config user.email "${GIT_USER_EMAIL:-dev@example.com}"

git add -A
git diff --cached --quiet || git commit -m "Lia AI project"

if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REPO_URL"
else
  git remote add origin "$REPO_URL"
fi

echo "Pushing to $REPO_URL ..."
echo "When asked for a password, paste your GitHub Personal Access Token (not your GitHub password)."
git push -u origin main
echo "Done. Open the repo's Actions tab to watch the build."
