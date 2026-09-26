# HR Native Android R2.0.7 — Premium App Icon Refresh

هذا تحديث خفيف فوق النسخة المستقرة R2.0.6، هدفه تحديث أيقونة التطبيق فقط مع رفع رقم الإصدار حتى يثبت APK الجديد فوق النسخة السابقة بدون حذف.

## المضاف

- أيقونة تطبيق أقوى بطابع رسمي للموارد البشرية.
- اعتماد ألوان نسخة الويب المرفوعة:
  - الأزرق الرسمي.
  - الكحلي.
  - الذهبي.
- توليد كل مقاسات Android داخل مجلدات `mipmap`.
- تحديث `versionCode` إلى `7`.
- تحديث `versionName` إلى `2.0.7`.
- تحديث اسم Artifact في GitHub Actions إلى:

```text
HRNativeAndroid-R2.0.7-premium-icon-signed-apk
```

## ملاحظة مهمة

يمكن اعتماد نسخة الويب كمرجع بصري دائم للتطبيق Native في التحديثات القادمة، بحيث نأخذ منها الألوان، ترتيب البطاقات، الخطوط، وأسلوب الواجهات، ثم نحولها إلى واجهات Android Native تدريجيًا.

## طريقة الرفع

انسخ محتويات هذا التحديث فوق مشروع `HRNativeAndroidR2`، ثم نفذ:

```cmd
git add -A
git commit -m "HR Native Android R2.0.7 premium app icon refresh"
git push origin main
```

بعد نجاح GitHub Actions حمّل:

```text
HRNativeAndroid-R2.0.7-premium-icon-signed-apk
```

ثم ثبّت `app-release.apk` فوق R2.0.6 مباشرة.
