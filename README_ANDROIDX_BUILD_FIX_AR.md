# AndroidX Build Fix

هذا الإصلاح يعالج فشل GitHub Actions في خطوة:

`Build signed release APK`

سبب الخطأ:

المشروع يحتوي اعتماد AndroidX:

`androidx.core:core:1.13.1`

لكن ملف `gradle.properties` كان يحتوي:

`android.useAndroidX=false`

لذلك يفشل Gradle برسالة:

`Configuration ':app:releaseRuntimeClasspath' contains AndroidX dependencies, but the android.useAndroidX property is not enabled`

## الإصلاح

تم تعديل `gradle.properties` إلى:

```properties
android.useAndroidX=true
android.suppressUnsupportedCompileSdk=35
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

هذا الإصلاح مناسب لـ R2.7.0 و R2.8.0 لأن المشكلة في إعداد عام وليس في شاشة معينة.

## طريقة التطبيق من VS Code

1. فك ضغط هذا Patch فوق مجلد المشروع.
2. نفذ:

```bash
git status
git add -A
git commit -m "Fix AndroidX build configuration"
git push
```

3. افتح GitHub Actions وانتظر البناء.

إذا صار البناء أخضر، حمّل Artifact وثبّت `app-release.apk` فقط.
