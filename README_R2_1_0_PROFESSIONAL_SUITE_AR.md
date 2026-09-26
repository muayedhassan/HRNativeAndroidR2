# HR Native Android R2.1.0 — Professional Suite Update

هذا تحديث كبير فوق R2.0.7، ويجمع ثلاث ترقيات في إصدار واحد حتى يكون التغيير واضحًا داخل التطبيق.

## محتوى التحديث

1. واجهة رئيسية تنفيذية جديدة
   - هيدر رسمي جديد.
   - إحصاءات مباشرة للموظفين والملاحظات.
   - وصول سريع للقائمة والملاحظات ولوحة المسؤول ومركز التحديث.

2. لوحة مسؤول النظام
   - مؤشرات مراجعة سريعة.
   - عدد الملاحظات الجديدة والمؤرشفة.
   - حالة بيانات الموظفين.
   - فتح سجل المراجعة مباشرة.

3. مركز تحديث داخل التطبيق
   - عرض رقم الإصدار الحالي.
   - تعليمات التحديث الثابتة.
   - تنبيه لاستخدام APK الناتج من GitHub Actions فقط.

4. هوية بصرية وخطوط Native
   - اعتماد ألوان نسخة الويب.
   - إضافة خطوط Android من نسخة الويب:
     - YaModernPro للعناوين.
     - ZainMobile للنصوص العربية.
     - Stencil للأرقام.

## الإصدار

- `versionCode`: 10
- `versionName`: 2.1.0
- `APP_VERSION`: R2.1.0

## Artifact المتوقع من GitHub Actions

```text
HRNativeAndroid-R2.1.0-professional-suite-signed-apk
```

وبداخله:

```text
app-release.apk
```

## آلية التحديث

1. فك ملف Patch zip فوق مشروع `HRNativeAndroidR2`.
2. نفّذ:

```cmd
git add -A
git commit -m "HR Native Android R2.1.0 professional suite update"
git push origin main
```

3. افتح GitHub Actions.
4. انتظر نجاح البناء.
5. حمّل Artifact.
6. ثبّت `app-release.apk` فوق R2.0.7 مباشرة.

مهم: استخدم APK الناتج من GitHub Actions فقط لأن توقيعه ثابت.
