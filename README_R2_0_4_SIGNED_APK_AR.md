# HR Native Android R2.0.4 — Signed APK ثابت التوقيع

هذا التحديث يضبط بناء التطبيق عبر GitHub Actions لإنتاج APK موقّع بتوقيع ثابت باستخدام GitHub Secrets.

## الأسرار المطلوبة في GitHub

يجب أن تكون هذه الأسرار موجودة داخل:
Settings → Secrets and variables → Actions

- KEYSTORE_BASE64
- KEYSTORE_PASSWORD
- KEY_ALIAS
- KEY_PASSWORD

## الناتج من GitHub Actions

بعد الرفع، ستجد Artifact باسم:

`HRNativeAndroid-R2.0.4-signed-apk`

وداخله ملف:

`app-release.apk`

## ملاحظة مهمة

إذا كانت النسخة المثبتة على الهاتف هي Debug APK من R2.0.3 أو أقل، فقد تحتاج حذفها مرة واحدة فقط، ثم تثبيت APK الموقّع R2.0.4. بعد ذلك، الإصدارات الموقعة القادمة ستثبت فوق R2.0.4 مباشرة بدون حذف.

لا ترفع ملفات التوقيع إلى GitHub:

- hr-native-release.jks
- keystore_base64.txt
