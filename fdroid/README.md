# F-Droid and IzzyOnDroid

GapWise has a third build, the **fdroid** flavor, made for free-software stores:

- No Google Play services or other proprietary libraries, so no Google Drive backup.
  Settings → Export to a file covers backups instead.
- No self-updater: the store handles updates.
- Signed with the same release key as the GitHub and Play builds, so people can move between
  them without reinstalling.
- **Reproducible.** The `Reproducible F-Droid build` workflow builds it on two different machines
  on every push and fails if the APKs differ. The Release workflow also checks that an unsigned
  rebuild matches the signed APK before it publishes anything.

Each GitHub release has `gapwise-vX.Y.Z-fdroid.apk`.

## Getting listed on F-Droid

1. Release a version that includes the fdroid flavor (v0.3.1 or later), using the Release workflow.
2. Fork https://gitlab.com/fdroid/fdroiddata and copy `com.yogeshpaliyal.gapwise.yml` from this
   folder to `metadata/com.yogeshpaliyal.gapwise.yml`. Set the `Builds` entry and
   `CurrentVersion`/`CurrentVersionCode` to the release you made. `commit` must be the full hash.
   Submitted as https://gitlab.com/fdroid/fdroiddata/-/merge_requests/58342.
3. Check it locally if you can: `fdroid readmeta`, `fdroid lint com.yogeshpaliyal.gapwise`, then
   `fdroid build -v -l com.yogeshpaliyal.gapwise`.
4. Open a merge request. Reviewers build it, and because of `Binaries` and `AllowedAPKSigningKeys`
   F-Droid publishes our signed APK once its own build matches.

After that, new tags are picked up automatically (`UpdateCheckMode: Tags`, `AutoUpdateMode: Version`).
The store listing (title, descriptions, icon, screenshots, and "What's new" from
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`) comes from this repo's
`fastlane/` folder.

## Getting listed on IzzyOnDroid

Open a request at https://github.com/IzzyOnDroid/repo/issues using the "app inclusion" template.
Point it at this repo and say the APK to use is the one matching `gapwise-v.*-fdroid\.apk` in
GitHub releases. IzzyOnDroid also runs reproducible-build checks; the same recipe as above
(fdroid flavor, `assembleFdroidRelease`, with JDK 17 or 21) reproduces it.

## Signing certificate

SHA-256 of the release certificate, the same for every GapWise build:

```
90:16:A6:8E:60:E5:DD:03:3A:EA:17:DD:D0:E9:A8:6C:BD:80:64:5F:E0:17:C8:32:A6:4C:35:B3:66:7B:76:A3
```
