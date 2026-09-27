# HR Native Android R2.14.0

## Web Layout Parity

هذا التحديث يركز على مطابقة واجهة نسخة الويب قدر الإمكان داخل تطبيق Android Native، دون إضافة أفكار جديدة بعيدة عن ترتيب الويب.

## ما تغير في الرئيسية

- أصبحت الواجهة الرئيسية مقسمة إلى قسمين رئيسيين:
  - نطاق السجل
  - أقسام البرنامج
- تم تقليل اللوحات الكثيرة المتفرقة في الرئيسية.
- أصبحت الكروت أوضح وأقرب لطريقة ترتيب نسخة الويب.

## قسم نطاق السجل

يحتوي على كروت مباشرة:

- كل السجل
- الدائميون
- العقود
- النواقص

كل كرت يفتح سجل الموظفين بالنطاق المناسب مباشرة.

## قسم أقسام البرنامج

يحتوي على كروت تشغيل للأقسام:

- سجل الموظفين
- ملاحظات المدير
- جودة البيانات
- لوحة المسؤول
- حالة النظام
- مركز التحديث

## ملفات الإصدار

- `versionCode`: 24
- `versionName`: 2.14.0
- Artifact في GitHub Actions:
  `HRNativeAndroid-R2.14.0-web-layout-parity-signed-apk`

## طريقة التحديث من VS Code

1. فك ملف Patch zip فوق مجلد المشروع `HRNativeAndroidR2`.
2. افتح Terminal داخل VS Code.
3. نفذ:

```bash
git add -A
git commit -m "R2.14.0 web layout parity"
git push
```

4. افتح GitHub Actions.
5. انتظر نجاح Build Android Signed APK.
6. حمّل Artifact:
   `HRNativeAndroid-R2.14.0-web-layout-parity-signed-apk`
7. افتح الملف داخله:
   `app-release.apk`
8. ثبّته فوق النسخة الموجودة على الهاتف.

مهم: استخدم فقط `app-release.apk` الناتج من GitHub Actions لأن التوقيع ثابت.
