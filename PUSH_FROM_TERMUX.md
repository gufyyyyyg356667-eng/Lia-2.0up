# Push Lia AI to GitHub from Termux

1. Create an empty private repository on GitHub named `lia-ai` (do not add a README).
2. Create a Personal Access Token: GitHub > Settings > Developer settings > Personal access tokens > Tokens (classic). Tick the `repo` and `workflow` scopes. Copy the token.
3. In Termux, unzip this project, then:

       cd Lia-AI
       bash push-to-github.sh https://github.com/YOUR_USERNAME/lia-ai.git

   When asked for a password, paste the token.

4. Open the repo on GitHub > Actions > "Build Lia AI". When it finishes green, download `lia-debug-apks` from the run page.

Secrets never go in the repository. `keystore.properties` and `license.properties` are git-ignored. For the direct build's licence server, add a repository secret `LICENSE_BASE_URL` (Settings > Secrets and variables > Actions).
