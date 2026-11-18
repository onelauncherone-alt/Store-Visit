# Site Visit Details — Android app

A simple Android app to record daily site visits and travel expenses.

## Features
- Add, edit and delete daily site-visit records.
- Dashboard shows total visits and total travel expenses.
- Calendar date picker; day of week is filled automatically.
- Month dropdown (January–December), synced to the selected date.
- Designation dropdown: ALPO, MLP, ELP, Other.
- Format/Store dropdown: IBBN, MDC.
- Auto/Bus and Train amounts; Total is calculated automatically.
- Records are saved locally on the Android phone using app preferences.

## Build APK using GitHub
1. Create a new GitHub repository.
2. Upload all files and folders from this project to the repository (keep folder structure).
3. Open the repository's **Actions** tab.
4. Select **Build Android APK** and click **Run workflow**. A push to `main` also starts a build.
5. Open the completed workflow run and download the `SiteVisitDetails-debug-apk` artifact.
6. Extract the artifact ZIP to get `app-debug.apk`.
7. Transfer the APK to your Android phone and install it. You may need to allow installation from that file source.

## Build locally
Open the project in Android Studio, allow Gradle sync, then run the app or choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Notes
- This project creates a debug APK for testing and direct installation. A Play Store release requires a separately signed release build.
- Data is stored on the device and is not automatically synced between phones.
- The month list covers all months, and the initial selected month follows the selected date.
