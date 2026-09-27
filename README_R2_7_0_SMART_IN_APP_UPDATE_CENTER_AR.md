# HR Native Android R2.7.0 — Smart In-App Update Center

هذا التحديث يضيف مركز تحديث ذكي داخل التطبيق، حتى لا تحتاج البحث اليدوي عن ملف APK في كل جهاز.

## ماذا يفعل التحديث

- يضيف زر `فحص التحديث` داخل مركز التحديث.
- يقرأ ملف:

```text
latest.json
```

من GitHub.

- يقارن `versionCode` المنشور مع رقم الإصدار المثبت على الهاتف.
- إذا كان هناك إصدار أحدث، يظهر زر `تحميل وتثبيت`.
- يحمّل ملف APK من الرابط الموجود في `apkUrl`.
- يفتح شاشة تثبيت Android مباشرة بعد اكتمال التحميل.

## مهم جدًا

Android لا يسمح بالتثبيت الصامت للتطبيقات خارج Google Play أو MDM.

لذلك بعد التحميل سيظهر للمستخدم مربع تثبيت Android، ويجب أن يوافق على التثبيت.

## ملف latest.json

تمت إضافة ملف `latest.json` في جذر المشروع.

الصيغة:

```json
{
  "versionCode": 17,
  "versionName": "R2.7.0",
  "artifactName": "HRNativeAndroid-R2.7.0-smart-in-app-update-center-signed-apk",
  "apkUrl": "",
  "notes": "R2.7.0 يضيف مركز تحديث ذكي داخل التطبيق. ضع هنا رابط APK مباشر عند نشر إصدار أحدث."
}
```

عند إصدار تحديث جديد لاحقًا، غيّر:

- `versionCode`
- `versionName`
- `artifactName`
- `apkUrl`
- `notes`

## رابط APK مباشر

يجب أن يكون `apkUrl` رابطًا مباشرًا يمكن للهاتف تحميله بدون تسجيل دخول.

الأفضل استخدام GitHub Releases ورفع `app-release.apk` كـ Release Asset، ثم وضع رابط التحميل المباشر في `apkUrl`.

ملاحظة: GitHub Actions Artifact غالبًا يحتاج جلسة GitHub ولا يكون دائمًا رابطًا مباشرًا مناسبًا للتطبيق.

## أرقام الإصدار

- `versionCode`: 17
- `versionName`: 2.7.0
- `APP_VERSION`: R2.7.0

## Artifact المتوقع من GitHub Actions

```text
HRNativeAndroid-R2.7.0-smart-in-app-update-center-signed-apk
```

داخله:

```text
app-release.apk
```

## آلية التحديث لهذه النسخة

1. فك ملف Patch zip فوق مشروع `HRNativeAndroidR2`.
2. نفذ:

```bash
git add -A
git commit -m "HR Native Android R2.7.0 smart in-app update center"
git push
```

3. افتح GitHub Actions وانتظر نجاح البناء.
4. حمّل Artifact وثبّت `app-release.apk` يدويًا هذه المرة.
5. بعد تثبيت R2.7.0، ستكون الأجهزة قادرة على فحص التحديثات القادمة من داخل التطبيق.

مهم: استخدم فقط APK الناتج من GitHub Actions لأن توقيعه ثابت.
