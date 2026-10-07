# Install guide (Play Protect "App blocked" fix)

Notification-listener apps ko Google Play Protect sideload par block kar sakta hai (khaas kar India me).

## Method 1: Play Protect temporarily OFF
1. Play Store -> profile icon -> **Play Protect** -> Settings (gear)
2. **Scan apps with Play Protect** OFF karo
3. APK install karo
4. Install ke baad Play Protect wapas ON kar sakte ho (app hatayega nahi)

## Method 2: ADB (PC se, Play Protect OFF kiye bina)
```
adb install NotificationHistory.apk
```

## Install ke baad
Settings -> Notifications -> Notification access -> Notification History ON.
Agar grey ho: App info -> 3 dots -> Allow restricted settings.
