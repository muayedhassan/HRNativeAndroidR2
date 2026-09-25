# HR Native Android R2.0.3 — Build Fix

هذا التصحيح يعالج فشل GitHub Actions في إصدار R2.0.3 بسبب خطأ صياغة داخل MainActivity.java في دالة infoRow.

## طريقة التركيب

انسخ محتويات هذا المجلد فوق مشروعك الحالي في VS Code، ثم نفذ:

```cmd
git add -A
git commit -m "HR Native Android R2.0.3 build fix"
git push origin main
```

بعدها افتح GitHub Actions وانتظر نجاح البناء، ثم حمّل Artifact الخاص بـ R2.0.3.
