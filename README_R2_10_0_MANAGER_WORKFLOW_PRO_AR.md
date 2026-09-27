# HR Native Android R2.10.0

## Manager Workflow Pro

هذا التحديث يركز على شاشة **ملاحظات مدير الموارد البشرية** ويحوّلها من نموذج بسيط إلى لوحة سير عمل احترافية.

## أهم التغييرات

- لوحة سير ملاحظات جديدة.
- خطوات واضحة داخل الشاشة:
  - الموظف
  - الحركة
  - السجل
- بطاقات اختيار الحركة بدل الأزرار الصغيرة:
  - ملاحظة
  - نقل
  - تنسيب
  - إنهاء تنسيب
- بطاقة الموظف المختار أصبحت أوضح وتعرض:
  - اسم الموظف
  - الشعبة الحالية
  - نوع التوظيف
- بطاقات سجل الملاحظات أصبحت أكثر وضوحًا للمراجعة.
- بقاء غرفة السيطرة من R2.9.0.
- بقاء بطاقة الموظف ذات التبويبات من R2.8.0.
- بقاء مركز التحديث الذكي من R2.7.0.
- إصلاح AndroidX مدمج.

## رقم الإصدار

- `versionCode`: 20
- `versionName`: 2.10.0
- اسم Artifact المتوقع من GitHub Actions:
  `HRNativeAndroid-R2.10.0-manager-workflow-pro-signed-apk`

## طريقة التحديث من VS Code

1. فك ضغط ملف Patch فوق مشروع `HRNativeAndroidR2`.
2. من Terminal داخل VS Code نفذ:

```bash
git status
git add -A
git commit -m "R2.10.0 manager workflow pro"
git push
```

3. افتح GitHub Actions وانتظر اكتمال البناء.
4. حمّل Artifact:
   `HRNativeAndroid-R2.10.0-manager-workflow-pro-signed-apk`
5. ثبّت الملف الموجود داخله:
   `app-release.apk`

مهم: استخدم APK الموقع الناتج من GitHub Actions فقط حتى يثبت فوق النسخة الموجودة بدون حذف.
