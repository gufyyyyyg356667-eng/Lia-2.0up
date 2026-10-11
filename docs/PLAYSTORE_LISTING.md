# Google Play listing: Lia AI (play flavor)

## Artifact
Build the signed Android App Bundle:

    ./gradlew bundlePlayRelease

Upload `app/build/outputs/bundle/playRelease/app-play-release.aab`. The signing values come from
`keystore.properties` (git-ignored). Never commit the keystore.

## Listing copy (draft)
- **Title:** Lia AI: Voice Assistant
- **Short description:** Talk to Lia in Hindi, Hinglish or English. Open apps, call and prepare messages.
- **Full description:** Lia is a voice-first assistant. Speak naturally and Lia answers in the language you use.
  It can open apps, call a contact, and open a WhatsApp or SMS message for you. Lia never sends a message
  without you tapping Send. Pick from ten personalities, and choose a theme and an orb style.
  You bring your own Gemini API key, which stays on your phone.

## Data safety answers (draft, verify before submitting)
- **Data collected:** audio from the microphone while a voice session is on; the text of typed chats;
  contacts only when you ask Lia to call or message someone (read on the phone, not uploaded by Lia).
- **Data shared:** audio and text are sent to Google's Gemini service to produce replies. Lia does not
  sell or share data with advertisers.
- **Stored on device:** the API key, settings, and chat history. Deleting chat history is available in Privacy.
- **Encryption in transit:** yes (HTTPS and WSS).
- **Users can request deletion:** delete chat history in-app; uninstall removes all local data.
- **No ads, no analytics SDK** in this build.

## Permission justifications (paste into the Play Console declarations)
- **Microphone (RECORD_AUDIO, foreground service type microphone):** the voice conversation needs the
  microphone while the user has started a voice session. A persistent notification with a Stop button is shown.
- **Contacts (READ_CONTACTS):** used only when the user asks to call or message someone by name. Results
  stay on the device.
- **Phone calls (CALL_PHONE):** used only when the user asks to call a contact. If not granted, Lia opens the dialer instead.
- **Notifications (POST_NOTIFICATIONS):** shows the ongoing voice-session notification.
- The `play` build does not use the Accessibility API, does not read other apps' screens, and does not
  request SMS sending (SEND_SMS). It does not request all-packages visibility.

## Submission checklist
- [ ] `./gradlew testPlayDebugUnitTest` and `./gradlew testDirectDebugUnitTest` pass.
- [ ] `scripts/smoke-test.sh play` passes on a real phone.
- [ ] Manual check: onboarding, key entry, voice start and stop, notification Stop button, chat, history, delete history.
- [ ] Release build is minified and the AAB opens on a clean device.
- [ ] Privacy policy URL is set in the Console and matches the data safety answers.
- [ ] Target audience and content rating completed. Not directed at children.
- [ ] Declarations for microphone foreground service and contacts/calls use the justifications above.
- [ ] Version code incremented in `app/build.gradle.kts`.
