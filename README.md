# Notification History (Android)

Har app ki har notification ko automatically save/record karne wala Android app - bilkul notification history ki tarah.

## Features
- Har app ki notification (title + text) local database me save hoti hai
- Notification delete/clear kar do to bhi history me rehti hai
- Search box + app-wise filter
- Notification par tap karke poora text dekho, long-press se delete
- CSV export (share kar sakte ho)
- Ongoing notifications (music / download progress) ignore karne ka option
- Sab data sirf phone me, internet permission nahi

## APK kaise milega
1. Is repo ke **Actions** tab me `Build APK` workflow chalta hai (har push par, ya manually `Run workflow`).
2. Complete hone par **Releases** section se `NotificationHistory.apk` download karo (ya workflow run ke Artifacts se).
3. Phone me install karo (Unknown sources allow karna padega).

## Setup
1. App kholo -> **Allow access** dabao -> list me *Notification History* ON karo.
2. Android 13+ par agar option grey ho: Settings -> Apps -> Notification History -> 3 dots -> *Allow restricted settings*, phir dobara ON karo.
3. Battery settings me app ko *Unrestricted* kar do, taaki background me kill na ho.

## Local build
```
gradle assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`
