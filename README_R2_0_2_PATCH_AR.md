# HR Native Android R2.0.2 — Real Data Foundation

هذا التحديث يضيف الأساس الحقيقي لبيانات الموظفين داخل تطبيق Android Native.

## المضاف

- قراءة بيانات الموظفين من GitHub:
  `https://raw.githubusercontent.com/muayedhassan/employees/main/data/employees.json`
- شاشة `القائمة` للبحث الفعلي في الموظفين.
- البحث يبدأ بعد كتابة حرفين أو أكثر، ولا يعرض كل الأسماء عند الضغط فقط.
- بطاقة موظف أولية تعرض:
  - الاسم
  - الرقم الوظيفي
  - الشعبة
  - العنوان الوظيفي
  - النوع: دائم / عقد
  - الدرجة / المرحلة
  - التحصيل
  - تاريخ التعيين
- حفظ نسخة محلية من البيانات داخل التطبيق.
- شاشة حالة النظام تعرض رابط البيانات وعدد الموظفين ونسخة البيانات.
- ملاحظات المدير بقيت محلية كمرحلة انتقالية، وسيتم ربط Google Sheet في R2.0.3.

## طريقة التركيب

انسخ محتويات هذا المجلد فوق مشروعك الحالي في VS Code، ثم نفذ:

```cmd
git add -A
git commit -m "HR Native Android R2.0.2 real data foundation"
git push origin main
```

بعدها ادخل GitHub Actions وحمل Artifact باسم:

`HRNativeAndroid-R2.0.2-debug-apk`
