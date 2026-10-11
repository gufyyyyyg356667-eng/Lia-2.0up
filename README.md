# Lia AI (Android)

Voice-first Android assistant: real-time Hindi / Hinglish / English voice over the Gemini Live API,
10 personalities, typed chat, and phone actions (open app, call, message).

Source of the design: `lia-build-guide`. Implemented so far: parts 01–15 minus the 3D scene (core voice/chat/actions, design system, 3D orb, splash, edge glow, screens, direct-only visual agent core and access-key gate) and some unit tests from part 15.

## Not implemented yet (from the guide)
- Part 09 gaps: Instrument Serif and Manrope font files are not bundled yet (system Serif/SansSerif is used), no tilt parallax on the night sky, no picker dialogs (rows are used instead).
- Part 14 gaps: the Three.js 3D scene (assets/forge) is not bundled, because the library file must be downloaded separately. Notifications and the share/save-to-device actions are not built; the Forge build screen is a plain status card, not the 3D scene. Only the result viewer is sandboxed.
- Part 10 gaps: the THINKING state is not yet produced by the session; the 3D orb needs a real device to check the tilt and shader.
- Part 11–11b: remaining screens, navigation, 3D screen look, bottom nav bar
- Part 12: Instagram/Facebook/WhatsApp visual agent (direct only)
- Part 13: access-key licensing (direct only)
- Part 14: Website Forge (3D site builder)
- Accessibility screen control (direct flavor). The `direct` flavor adds only the system overlay for now.
- DataStore instead of SharedPreferences, and encrypted storage for the API key.

## Setup
1. Install Android Studio (Ladybug or newer) with Android SDK 36.
2. Open the `Lia-AI` folder. Android Studio creates the Gradle wrapper and `local.properties`.
   (Or run `gradle wrapper --gradle-version 8.9` once.)
3. Build:
   - `./gradlew assembleDirectDebug` and `./gradlew assemblePlayDebug`
4. Tests: `./gradlew testDirectDebugUnitTest testPlayDebugUnitTest`
5. Install, open the app, paste a Gemini API key in the setup screen. The key stays on the phone.

## Release
Copy `keystore.properties.example` to `keystore.properties` and fill it in. That file and any `.keystore` are git-ignored.

## Notes
- The Live model name is one constant in `GeminiLiveClient.MODEL`. Check the current name in the Gemini Live docs.
- Android may block starting activities from the background. Test the phone actions on a real device.
- Nothing here has been compiled or run on a device yet.

## Direct build licensing (Part 13)
Create `license.properties` (git-ignored) from `license.properties.example` and set `licenseBaseUrl` to your key server. Without it, the direct build reports "Licensing is not set up in this build" and refuses voice and tools.
The gate is client-side. It stops ordinary users, not someone who rebuilds the app. Real enforcement needs server-side checks.

## Tests
- Common: `./gradlew testPlayDebugUnitTest` runs the shared logic tests in `src/test` plus the Play policy tests.
- Direct: `./gradlew testDirectDebugUnitTest` runs the same plus the direct-only agent and licence tests.

## Smoke test (real phone)
`scripts/smoke-test.sh play` or `scripts/smoke-test.sh direct` installs the debug build, launches it and checks for crashes.

## Release
- `./gradlew bundlePlayRelease` for the Play upload. `./gradlew assembleDirectRelease` for the direct APK.
- Signing values go in `keystore.properties` (copy from the example). See `docs/PLAYSTORE_LISTING.md`.

## Direct build: screen control (Part 07)
The direct build includes an accessibility service. Turn it on in Settings > Accessibility > Lia AI screen control, or from Permissions. Lia reads only labels on the active screen, and only when you ask it to. The Play build does not include this service.

## Still not built
- The Three.js 3D Forge scene (needs the Three.js file added to assets/forge).
- Splash particle intro and onboarding page poses.
- Compile fixes: the project has not been compiled yet.
