# Lia AI: remaining features

## Done in code (check on device after build)
- Phone control: torch, volume, alarms, timers, settings pages, links, screenshot, lock, notifications, quick settings, media, ringer, brightness, camera, navigation, recent apps, share, dialer, SMS and WhatsApp prepare (user presses send), clipboard, auto-rotate, calendar event, contact, email prepare, gallery, app info, notification settings, wallpaper, alarms, contacts.
- Screen agent: read, tap (scroll to find), type, press Enter, search inside an app, multi-step tasks split into ordered steps, background tasks reported back by voice.
- Alerts: charger connected/removed, full charge, low battery (15%, 10%), headphones, network lost/back, download finished, storage nearly full, phone hot.
- Information: weather (Open-Meteo), nearest pharmacy (OpenStreetMap), calendar read, calendar event starting in 15 minutes, morning briefing, reminders (one-time and daily), emergency dialer (user presses Call).

## Not yet built
1. **Screen time and daily summary:** needs Usage access (special permission the user grants in Settings). Then: screen time today and top apps.
2. **Travel time and traffic:** needs a Google Directions API key. Then: "office-e koto somoy lagbe".
3. **Read notifications (optional, explicit opt-in):** Notification access. Only for chosen apps, never message bodies stored.
4. **Bill reminders with due dates:** a form in the app to add bill name, amount, due date. Then a reminder before the due date.
5. **Routines:** a named sequence, for example "Good night" = lower brightness, silent, set alarm 7 AM.
6. **Smart reminders after a timer or alarm:** when a timer ends, Lia asks a follow-up question.
7. **Background reliability:** on some phones (Xiaomi, Oppo, Vivo, Realme) the monitor stops; a guided "keep running" screen per brand.
8. **Full offline voice:** works without internet for device controls. Gemini Live still needs internet.
9. **Multi-language phone replies:** Hindi and English alerts, following the language setting.
10. **Test automation:** scripted device tests for the main flows, so each build is checked before install.

## Cannot be done by Android apps
- Turning Wi-Fi or Bluetooth on or off directly (Android no longer allows it; Lia can only open the settings page).
- Reading other apps' private data without the user's explicit permission.
