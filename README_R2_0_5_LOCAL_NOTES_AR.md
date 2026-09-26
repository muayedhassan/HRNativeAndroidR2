# HR Native Android R2.0.5 — Local Manager Notes Persistence

هذا تحديث فعلي صغير فوق R2.0.4 لاختبار التثبيت الموقّع فوق النسخة السابقة، مع إضافة فائدة عملية:

- حفظ ملاحظات المدير محليًا داخل التطبيق.
- حفظ حالة "تمت المراجعة" محليًا.
- بقاء سجل الملاحظات بعد إغلاق التطبيق وإعادة فتحه.
- تحديث الإصدار إلى R2.0.5.
- استمرار إخراج APK موقّع عبر GitHub Actions بالاسم: HRNativeAndroid-R2.0.5-signed-apk.

## طريقة التركيب

انسخ محتويات هذا المجلد فوق مشروعك الحالي، ثم نفذ:

```cmd
git add -A
git commit -m "HR Native Android R2.0.5 local manager notes persistence"
git push origin main
```

بعد نجاح GitHub Actions حمّل app-release.apk وثبّته فوق R2.0.4 مباشرة دون حذف التطبيق.
