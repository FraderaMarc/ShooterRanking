Shooter Ranking - AdMob patch
================================

Replace the files in your current project with the files in this archive.

Changes included:
- Real AdMob App ID in debug and release so UMP can retrieve your consent message.
- Debug uses Google's official anchored adaptive banner test ID.
- Release uses your real banner ID.
- UMP consent is gathered before Mobile Ads initialization.
- Privacy options appear inside Legal information when UMP requires an entry point.
- Google Mobile Ads SDK kept at 23.6.0 for compatibility with Kotlin 2.0.20.
- AGP updated to 8.10.1 for API 36 support.

No Firebase/Auth/Firestore/ranking/PDF/account-deletion functionality is changed.

After copying:
1. Sync Project with Gradle Files.
2. Build the debug version.
3. Verify the banner says Test Ad.
4. Test the consent form.
5. Build a release bundle only when ready for Play.
