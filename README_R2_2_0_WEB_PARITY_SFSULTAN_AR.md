# HR Native Android R2.2.0 — Web Parity + SF Sultan

هذا تحديث كبير فوق R2.1.0، وتم بناؤه بعد مراجعة نسخة الويب المرفوعة حتى يصبح تطبيق Android Native أقرب إلى تطبيق الويب نفسه.

## محتوى التحديث

1. إضافة خط SF Sultan
   - أضيف الخط إلى مشروع Android داخل:

```text
app/src/main/assets/fonts/SFSultan-Black.ttf
```

   - أصبح SF Sultan خط النصوص العربية داخل التطبيق.
   - بقي YaModernPro للعناوين.
   - بقي Stencil للأرقام.

2. تقريب الواجهة من نسخة الويب
   - إضافة نطاقات رئيسية مثل الويب:
     - الدائميون.
     - العقود.
     - الإدارة.
   - فتح الدائميين أو العقود مباشرة من الرئيسية.

3. فلاتر Native لقائمة الموظفين
   - الكل.
   - دائم.
   - عقد.
   - البحث يعمل داخل النطاق المختار.

4. استمرار مكونات R2.1.0
   - لوحة مسؤول النظام.
   - مركز التحديث.
   - الواجهة التنفيذية.
   - الأيقونة الاحترافية.

## الإصدار

- `versionCode`: 12
- `versionName`: 2.2.0
- `APP_VERSION`: R2.2.0

## Artifact المتوقع

```text
HRNativeAndroid-R2.2.0-web-parity-sfsultan-signed-apk
```

## آلية التحديث

1. فك ملف Patch zip فوق مشروع `HRNativeAndroidR2`.
2. نفّذ:

```cmd
git add -A
git commit -m "HR Native Android R2.2.0 web parity SF Sultan update"
git push origin main
```

3. افتح GitHub Actions.
4. انتظر نجاح البناء.
5. حمّل Artifact.
6. ثبّت `app-release.apk` فوق R2.1.0 مباشرة.

مهم: يجب استخدام APK الناتج من GitHub Actions فقط لأن توقيعه ثابت ويثبت فوق النسخة السابقة.
