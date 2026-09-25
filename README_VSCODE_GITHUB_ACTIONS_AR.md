# تشغيل HR Native Android R2.0.1 بدون Android Studio

هذه النسخة مجهزة للعمل من خلال VS Code فقط، والبناء يتم على GitHub Actions.

## البرامج المطلوبة على جهازك

- VS Code
- Git
- حساب GitHub

لا تحتاج Android Studio ولا تحتاج Gradle على جهازك.

## طريقة الرفع لأول مرة

افتح VS Code على مجلد المشروع ثم افتح Terminal ونفذ:

```cmd
git init
git branch -M main
git add -A
git commit -m "HR Native Android R2.0.1 starter UI"
git remote add origin https://github.com/USERNAME/HRNativeAndroidR2.git
git push -u origin main
```

استبدل `USERNAME` باسم حسابك في GitHub.

## طريقة بناء APK

بعد الرفع:

1. افتح مستودع GitHub.
2. ادخل إلى تبويب Actions.
3. افتح آخر عملية باسم Build Android APK.
4. انتظر حتى تظهر علامة النجاح.
5. انزل إلى Artifacts.
6. حمل الملف: `HRNativeAndroid-R2.0.1-debug-apk`.
7. فك الضغط وستجد داخله `app-debug.apk`.
8. انقل APK إلى الهاتف وثبته.

## طريقة تحديث التطبيق لاحقًا

بعد أي تعديل من VS Code:

```cmd
git add -A
git commit -m "Native R2 update"
git push origin main
```

ثم ارجع إلى GitHub Actions وحمل APK الجديد.
