# HR Native Android R2.6.0 — Web Replica Native Layout

هذا التحديث يغيّر الاتجاه من تحسينات شكلية تدريجية إلى تقليد فعلي لتدفق نسخة الويب داخل Android Native.

## المشكلة التي يعالجها

الإصدارات السابقة حسّنت الشكل، لكنها بقيت بعيدة عن طريقة عمل نسخة الويب. لذلك تم تحويل الشاشة الرئيسية نفسها إلى سجل مباشر، بحيث لا يبدأ التطبيق بلوحة اختصارات فقط.

## التغيير الكبير

- الشاشة الرئيسية أصبحت مثل نسخة الويب:
  - هيدر رسمي.
  - تبويبات رئيسية.
  - شريط إحصاءات.
  - تبويبات فرعية.
  - بحث مباشر.
  - قائمة موظفين تظهر فورًا.
- لم يعد المستخدم يحتاج فتح “قائمة الموظفين” حتى يرى السجل.
- القائمة تعرض أول الموظفين ضمن النطاق الحالي مباشرة.
- البحث يصفّي النتائج داخل نفس الشاشة.
- التبويبات الرئيسية:
  - الدائميون.
  - العقود.
  - الكل.
- التبويبات الفرعية:
  - القائمة.
  - ملاحظات المدير.
  - الإدارة.
  - التحديثات.
  - النظام.

## ما بقي Native

- التطبيق لا يستخدم WebView.
- الواجهة مكتوبة Native Java.
- الأيقونات Native Vector.
- الخطوط مستمرة:
  - `SF Sultan` للنصوص العربية.
  - `Ya Modern Pro` للعناوين.
  - `Stencil` للأرقام.

## أرقام الإصدار

- `versionCode`: 16
- `versionName`: 2.6.0
- `APP_VERSION`: R2.6.0

## Artifact المتوقع من GitHub Actions

```text
HRNativeAndroid-R2.6.0-web-replica-native-layout-signed-apk
```

داخله:

```text
app-release.apk
```

## آلية التحديث المعتمدة

1. فك ملف Patch zip فوق مشروع `HRNativeAndroidR2`.
2. نفذ:

```bash
git add -A
git commit -m "HR Native Android R2.6.0 web replica native layout"
git push
```

3. افتح GitHub Actions وانتظر نجاح البناء.
4. حمّل Artifact:

```text
HRNativeAndroid-R2.6.0-web-replica-native-layout-signed-apk
```

5. ثبّت `app-release.apk` فوق النسخة السابقة مباشرة.

مهم: استخدم فقط APK الناتج من GitHub Actions لأن توقيعه ثابت.
