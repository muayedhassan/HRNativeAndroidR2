package iq.hr.mobile.nativeapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String APP_VERSION = "R2.0.6";
    private static final String DATA_URL = "https://raw.githubusercontent.com/muayedhassan/employees/main/data/employees.json";
    private static final String CACHE_FILE = "employees_cache_r2.json";
    private static final String NOTES_CACHE_FILE = "manager_notes_cache_r2.json";
    private static final String PREFS_FILE = "hr_native_preferences_r2";
    private static final String ROLE_ADMIN = "system_admin";
    private static final String ROLE_HR = "hr_manager";

    private static final int BG = Color.rgb(242, 246, 252);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(18, 30, 52);
    private static final int MUTED = Color.rgb(105, 117, 135);
    private static final int PRIMARY = Color.rgb(25, 76, 160);
    private static final int GREEN = Color.rgb(20, 150, 96);
    private static final int ORANGE = Color.rgb(220, 132, 28);
    private static final int PURPLE = Color.rgb(105, 72, 190);
    private static final int RED = Color.rgb(202, 58, 70);
    private static final int BORDER = Color.rgb(218, 226, 239);
    private static final int NAVY = Color.rgb(16, 40, 84);
    private static final int SOFT_BLUE = Color.rgb(235, 242, 255);

    private LinearLayout root;
    private String currentRole = ROLE_ADMIN;
    private String currentMovement = "ملاحظة";
    private Employee selectedEmployee;
    private final List<Employee> employees = new ArrayList<>();
    private final List<ManagerNote> notes = new ArrayList<>();
    private LinearLayout notesContainer;
    private String currentFilter = "الكل";
    private String dataVersion = "بيانات نموذجية";
    private String lastSync = "لم تتم مزامنة فعلية بعد";
    private int permCount = 0;
    private int contCount = 0;
    private boolean isSyncing = false;
    private boolean roleConfigured = false;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        seedFallbackData();
        loadCachedEmployees();
        loadCachedNotes();
        if (notes.isEmpty()) seedNotes();
        loadDeviceRole();
        if (!roleConfigured) {
            showDeviceRoleSetup();
        } else {
            showHome();
        }
    }

    private void seedFallbackData() {
        employees.clear();
        employees.add(new Employee("1001", "أحمد محمد حسن", "شعبة الموارد البشرية", "دائم", "موظف", "", "", "", "", "", "", ""));
        employees.add(new Employee("1002", "علي حسين كاظم", "شعبة الحسابات", "دائم", "موظف", "", "", "", "", "", "", ""));
        employees.add(new Employee("1003", "سجاد حسن جبار", "شعبة تكنولوجيا المعلومات", "عقد", "موظف", "", "", "", "", "", "", ""));
        employees.add(new Employee("1004", "زهراء عبد الكريم", "شعبة التخطيط", "دائم", "موظف", "", "", "", "", "", "", ""));
        employees.add(new Employee("1005", "مصطفى صالح مهدي", "شعبة المتابعة", "عقد", "موظف", "", "", "", "", "", "", ""));
        permCount = 3;
        contCount = 2;
    }

    private void seedNotes() {
        if (!notes.isEmpty()) return;
        notes.add(new ManagerNote("MN-10001", "نقل", "أحمد محمد حسن", "شعبة الحسابات", "شعبة الموارد البشرية", "يرجى اتخاذ ما يلزم بخصوص النقل.", "new", now()));
        notes.add(new ManagerNote("MN-10002", "تنسيب", "زهراء عبد الكريم", "شعبة التخطيط", "شعبة الموارد البشرية", "تنسيب مؤقت لمدة شهر.", "reviewed", now()));
    }

    private void baseScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(30));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setTextDirection(View.TEXT_DIRECTION_RTL);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        setContentView(scroll);
    }

    private void loadDeviceRole() {
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE);
        String savedRole = prefs.getString("deviceRole", "");
        roleConfigured = prefs.getBoolean("roleConfigured", false);
        if (ROLE_ADMIN.equals(savedRole) || ROLE_HR.equals(savedRole)) {
            currentRole = savedRole;
        } else {
            currentRole = ROLE_ADMIN;
            roleConfigured = false;
        }
    }

    private void saveDeviceRole(String role) {
        currentRole = ROLE_HR.equals(role) ? ROLE_HR : ROLE_ADMIN;
        roleConfigured = true;
        getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
                .edit()
                .putString("deviceRole", currentRole)
                .putBoolean("roleConfigured", true)
                .apply();
    }

    private void showDeviceRoleSetup() {
        baseScreen();
        LinearLayout header = premiumHeader("تهيئة الجهاز", "حدد صلاحية هذا الهاتف مرة واحدة، ثم سيعمل التطبيق بهذه الهوية تلقائيًا");
        root.addView(header);
        root.addView(space(14));

        LinearLayout box = card(20);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.addView(text("اختر نوع الجهاز", 20, TEXT, true));
        box.addView(space(8));
        box.addView(text("مسؤول النظام يراجع ويؤرشف، ومدير الموارد البشرية يرسل الملاحظات ويبحث عن الموظفين.", 13, MUTED, false));
        box.addView(space(14));

        final String[] selectedRole = { currentRole == null || currentRole.length() == 0 ? ROLE_ADMIN : currentRole };
        TextView selected = text("الاختيار الحالي: " + (ROLE_ADMIN.equals(selectedRole[0]) ? "مسؤول النظام" : "مدير الموارد البشرية"), 14, PRIMARY, true);

        Button admin = primaryButton("مسؤول النظام");
        Button hr = outlineButton("مدير الموارد البشرية");
        admin.setOnClickListener(v -> {
            selectedRole[0] = ROLE_ADMIN;
            selected.setText("الاختيار الحالي: مسؤول النظام");
        });
        hr.setOnClickListener(v -> {
            selectedRole[0] = ROLE_HR;
            selected.setText("الاختيار الحالي: مدير الموارد البشرية");
        });
        LinearLayout choices = horizontal();
        choices.addView(admin, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        choices.addView(spaceW(8));
        choices.addView(hr, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(choices);
        box.addView(space(10));
        box.addView(selected);
        box.addView(space(12));

        EditText pin = editText("رمز التفعيل");
        pin.setSingleLine(true);
        box.addView(pin);
        box.addView(space(8));
        box.addView(text("رمز مسؤول النظام: 1001 — رمز مدير الموارد البشرية: 2002", 12, MUTED, false));
        box.addView(space(14));

        Button save = primaryButton("حفظ هوية الجهاز");
        save.setOnClickListener(v -> {
            String entered = pin.getText().toString().trim();
            boolean ok = (ROLE_ADMIN.equals(selectedRole[0]) && "1001".equals(entered))
                    || (ROLE_HR.equals(selectedRole[0]) && "2002".equals(entered));
            if (!ok) {
                Toast.makeText(this, "رمز التفعيل غير صحيح", Toast.LENGTH_SHORT).show();
                return;
            }
            saveDeviceRole(selectedRole[0]);
            Toast.makeText(this, "تم حفظ هوية الجهاز: " + roleLabel(), Toast.LENGTH_SHORT).show();
            showHome();
        });
        box.addView(save);
        root.addView(box);
    }

    private void showHome() {
        baseScreen();

        LinearLayout hero = premiumHeader("نظام الموارد البشرية", "Android Native " + APP_VERSION + " — تصميم احترافي وهوية جهاز ثابتة");
        hero.addView(space(12));
        LinearLayout roleBar = horizontal();
        roleBar.addView(badge("نوع الجهاز: " + roleLabel(), currentRole.equals(ROLE_ADMIN) ? PRIMARY : PURPLE));
        roleBar.addView(spaceW(8));
        Button resetRole = outlineButton("إعادة تهيئة الجهاز");
        resetRole.setOnClickListener(v -> showDeviceRoleSetup());
        roleBar.addView(resetRole, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(roleBar);
        hero.addView(space(12));
        hero.addView(dataStatusMini());
        root.addView(hero);

        root.addView(space(14));
        root.addView(sectionTitle("لوحة البرامج — تصميم R2.0.6"));

        LinearLayout grid1 = horizontal();
        grid1.addView(dashboardCard("القائمة", "بحث مباشر وبطاقة موظف", PRIMARY));
        grid1.addView(spaceW(10));
        grid1.addView(dashboardCard("ملاحظات المدير", "هوية جهاز وحفظ محلي", PURPLE));
        root.addView(grid1);

        root.addView(space(10));
        LinearLayout grid2 = horizontal();
        grid2.addView(dashboardCard("التقارير", "PDF وتقارير الشعب", GREEN));
        grid2.addView(spaceW(10));
        grid2.addView(dashboardCard("حالة النظام", "مزامنة وتشخيص", ORANGE));
        root.addView(grid2);

        root.addView(space(16));
        Button listBtn = primaryButton("فتح قائمة الموظفين");
        listBtn.setOnClickListener(v -> showEmployeeDirectory());
        root.addView(listBtn);
        root.addView(space(10));
        Button syncBtn = outlineButton(isSyncing ? "جاري تحديث البيانات..." : "تحديث بيانات الموظفين من GitHub");
        syncBtn.setEnabled(!isSyncing);
        syncBtn.setOnClickListener(v -> syncEmployees(true));
        root.addView(syncBtn);

        root.addView(space(12));
        TextView footer = text("R2.0.6 تحديث فعلي: تصميم احترافي، أيقونة جديدة، وتثبيت نوع الجهاز بصلاحيات واضحة.", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
    }

    private LinearLayout premiumHeader(String titleText, String subtitleText) {
        LinearLayout header = card(22);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        header.setBackground(round(SOFT_BLUE, 22, Color.rgb(197, 214, 244)));
        LinearLayout top = horizontal();
        TextView logo = text("HR", 18, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setPadding(dp(12), dp(8), dp(12), dp(8));
        logo.setBackground(round(PRIMARY, 18, PRIMARY));
        top.addView(logo);
        top.addView(spaceW(10));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        titles.addView(text(titleText, 24, NAVY, true));
        titles.addView(space(4));
        titles.addView(text(subtitleText, 13, MUTED, false));
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(top);
        return header;
    }

    private LinearLayout dataStatusMini() {
        LinearLayout box = card(14);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView a = text("الموظفون: " + employees.size() + "  |  دائم: " + permCount + "  |  عقود: " + contCount, 13, TEXT, true);
        TextView b = text("البيانات: " + dataVersion + "\nآخر تحديث: " + lastSync, 11, MUTED, false);
        box.addView(a);
        box.addView(space(5));
        box.addView(b);
        return box;
    }

    private void showEmployeeDirectory() {
        baseScreen();

        LinearLayout header = premiumHeader("القائمة الرئيسية للموظفين", "بحث فعلي من بيانات GitHub مع بطاقة موظف تفصيلية ونسخة محلية احتياطية");
        header.addView(space(12));
        LinearLayout badges = horizontal();
        badges.addView(badge(employees.size() + " موظف", PRIMARY));
        badges.addView(spaceW(8));
        badges.addView(badge("دائم " + permCount, GREEN));
        badges.addView(spaceW(8));
        badges.addView(badge("عقود " + contCount, ORANGE));
        header.addView(badges);
        root.addView(header);

        root.addView(space(12));
        LinearLayout searchCard = card(18);
        searchCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        searchCard.addView(text("بحث الموظفين", 18, TEXT, true));
        searchCard.addView(space(6));
        searchCard.addView(text("اكتب حرفين أو أكثر من الاسم أو الرقم الوظيفي أو الشعبة. لا يتم عرض كل الأسماء عند الضغط فقط.", 12, MUTED, false));
        searchCard.addView(space(10));
        EditText search = editText("اكتب اسم الموظف أو الرقم الوظيفي...");
        search.setSingleLine(true);
        searchCard.addView(search);
        searchCard.addView(space(10));
        TextView status = text("جاهز للبحث", 12, MUTED, false);
        searchCard.addView(status);
        root.addView(searchCard);

        root.addView(space(10));
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);
        renderEmployeeSearchResults("", results, status);

        search.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && search.getText().toString().trim().length() < 2) {
                results.removeAllViews();
                results.addView(emptyCard("اكتب حرفين أو أكثر حتى تظهر النتائج"));
                status.setText("لا يتم عرض جميع الأسماء تلقائيًا");
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderEmployeeSearchResults(s.toString(), results, status);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        root.addView(space(14));
        Button sync = primaryButton(isSyncing ? "جاري تحديث البيانات..." : "تحديث البيانات الآن");
        sync.setEnabled(!isSyncing);
        sync.setOnClickListener(v -> syncEmployees(true));
        root.addView(sync);
        root.addView(space(8));
        Button back = outlineButton("الرجوع إلى الرئيسية");
        back.setOnClickListener(v -> showHome());
        root.addView(back);
    }

    private void renderEmployeeSearchResults(String query, LinearLayout container, TextView status) {
        container.removeAllViews();
        String q = normalize(query);
        if (q.length() < 2) {
            container.addView(emptyCard("اكتب حرفين أو أكثر للبحث في " + employees.size() + " موظف"));
            status.setText("البيانات الجاهزة: " + employees.size() + " موظف");
            return;
        }
        int shown = 0;
        int matched = 0;
        for (Employee e : employees) {
            if (employeeMatches(e, q)) {
                matched++;
                if (shown < 50) {
                    container.addView(employeeCard(e));
                    container.addView(space(8));
                    shown++;
                }
            }
        }
        if (matched == 0) {
            container.addView(emptyCard("لا توجد نتائج مطابقة"));
        }
        status.setText("النتائج: " + matched + (matched > 50 ? " — عُرضت أول 50 نتيجة فقط" : ""));
    }

    private LinearLayout employeeCard(Employee e) {
        LinearLayout c = card(18);
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        c.setOnClickListener(v -> showEmployeeProfile(e));

        LinearLayout top = horizontal();
        top.addView(badge(e.typeLabel(), "عقد".equals(e.typeLabel()) ? ORANGE : GREEN));
        top.addView(spaceW(8));
        top.addView(badge("رقم: " + safe(e.id), PRIMARY));
        c.addView(top);
        c.addView(space(8));
        c.addView(text(e.name, 18, TEXT, true));
        c.addView(space(7));
        c.addView(text("الشعبة: " + safe(e.branch), 13, MUTED, false));
        c.addView(text("العنوان الوظيفي: " + safe(e.jobTitle), 13, MUTED, false));
        c.addView(text("الدرجة/المرحلة: " + safe(e.grade) + " / " + safe(e.step), 13, MUTED, false));
        c.addView(text("التحصيل: " + safe(e.education), 13, MUTED, false));
        c.addView(text("تاريخ التعيين: " + shortDate(e.hireDate), 13, MUTED, false));
        c.addView(space(10));

        LinearLayout actions = horizontal();
        Button open = primaryButton("فتح بطاقة الموظف");
        open.setOnClickListener(v -> showEmployeeProfile(e));
        actions.addView(open, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(spaceW(8));
        Button choose = outlineButton("اختيار للملاحظات");
        choose.setOnClickListener(v -> {
            selectedEmployee = e;
            currentRole = ROLE_HR;
            Toast.makeText(this, "تم اختيار الموظف للملاحظات", Toast.LENGTH_SHORT).show();
            showManagerNotes();
        });
        actions.addView(choose, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c.addView(actions);
        return c;
    }

    private void showEmployeeProfile(Employee e) {
        baseScreen();

        LinearLayout header = card(20);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout top = horizontal();
        top.addView(badge(e.typeLabel(), "عقد".equals(e.typeLabel()) ? ORANGE : GREEN));
        top.addView(spaceW(8));
        top.addView(badge("رقم وظيفي: " + safe(e.id), PRIMARY));
        header.addView(top);
        header.addView(space(10));
        header.addView(text(e.name, 22, TEXT, true));
        header.addView(space(6));
        header.addView(text(safe(e.branch), 14, MUTED, false));
        header.addView(space(6));
        header.addView(text(safe(e.jobTitle), 14, MUTED, false));
        root.addView(header);

        root.addView(space(12));
        root.addView(infoSection("المعلومات الوظيفية",
                infoRow("الشعبة", safe(e.branch))
                        + infoRow("العنوان الوظيفي", safe(e.jobTitle))
                        + infoRow("الحالة", safe(e.type))
                        + infoRow("الدرجة", safe(e.grade))
                        + infoRow("المرحلة", safe(e.step))
                        + infoRow("الراتب", formatSalary(e.salary))
        ));

        root.addView(space(10));
        root.addView(infoSection("المعلومات الشخصية والهوية",
                infoRow("اسم الأم", safe(e.motherName))
                        + infoRow("الجنس", safe(e.gender))
                        + infoRow("رقم الهوية", safe(e.identityNo))
                        + infoRow("جهة الإصدار", safe(e.identityIssuer))
                        + infoRow("تاريخ الإصدار", shortDate(e.identityIssueDate))
                        + infoRow("تاريخ الولادة", shortDate(e.birthDate))
        ));

        root.addView(space(10));
        root.addView(infoSection("التحصيل والتخصص",
                infoRow("التحصيل", safe(e.education))
                        + infoRow("الاختصاص", safe(e.specialization))
        ));

        root.addView(space(10));
        root.addView(infoSection("التعيين والملاحظات",
                infoRow("تاريخ التعيين", shortDate(e.hireDate))
                        + infoRow("آخر تحديث للبيانات", shortDate(e.sourceUpdatedAt))
                        + infoRow("ملاحظات", safe(e.notes))
        ));

        root.addView(space(14));
        Button note = primaryButton("اختيار الموظف في ملاحظات المدير");
        note.setOnClickListener(v -> {
            selectedEmployee = e;
            currentRole = ROLE_HR;
            showManagerNotes();
        });
        root.addView(note);

        root.addView(space(8));
        Button backList = outlineButton("الرجوع إلى القائمة");
        backList.setOnClickListener(v -> showEmployeeDirectory());
        root.addView(backList);

        root.addView(space(8));
        Button backHome = outlineButton("الرجوع إلى الرئيسية");
        backHome.setOnClickListener(v -> showHome());
        root.addView(backHome);
    }

    private LinearLayout infoSection(String title, String body) {
        LinearLayout box = card(18);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.addView(text(title, 17, TEXT, true));
        box.addView(space(8));
        TextView content = text(body, 13, TEXT, false);
        content.setLineSpacing(dp(2), 1.0f);
        box.addView(content);
        return box;
    }

    private String infoRow(String label, String value) {
        return label + ": " + safe(value) + "\n";
    }

    private String formatSalary(String value) {
        String v = safe(value);
        if ("-".equals(v)) return v;
        try {
            long n = Long.parseLong(v.replace(",", "").trim());
            return String.format(Locale.US, "%,d", n) + " دينار";
        } catch (Exception ignored) {
            return v;
        }
    }

    private void showStatus() {
        baseScreen();
        LinearLayout header = premiumHeader("حالة النظام", "تشخيص نسخة Android Native ومزامنة بيانات الموظفين");
        root.addView(header);
        root.addView(space(12));
        LinearLayout box = card(18);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.addView(text("الإصدار: " + APP_VERSION, 14, TEXT, true));
        box.addView(space(6));
        box.addView(text("رابط البيانات:\n" + DATA_URL, 12, MUTED, false));
        box.addView(space(6));
        box.addView(text("عدد الموظفين المحلي: " + employees.size(), 13, TEXT, false));
        box.addView(text("دائم: " + permCount + " — عقود: " + contCount, 13, TEXT, false));
        box.addView(text("نسخة البيانات: " + dataVersion, 13, MUTED, false));
        box.addView(text("آخر تحديث: " + lastSync, 13, MUTED, false));
        root.addView(box);
        root.addView(space(12));
        Button sync = primaryButton(isSyncing ? "جاري تحديث البيانات..." : "تحديث من GitHub");
        sync.setEnabled(!isSyncing);
        sync.setOnClickListener(v -> syncEmployees(true));
        root.addView(sync);
        root.addView(space(8));
        Button back = outlineButton("الرجوع إلى الرئيسية");
        back.setOnClickListener(v -> showHome());
        root.addView(back);
    }

    private void showManagerNotes() {
        baseScreen();

        LinearLayout header = premiumHeader("ملاحظات مدير الموارد البشرية", "متابعة النقل، التنسيب، إنهاء التنسيب، والملاحظات الإدارية");
        header.addView(space(14));

        LinearLayout row = horizontal();
        row.addView(badge(roleLabel(), currentRole.equals(ROLE_ADMIN) ? PRIMARY : PURPLE));
        row.addView(spaceW(8));
        row.addView(badge(APP_VERSION + " Native", GREEN));
        header.addView(row);
        root.addView(header);

        root.addView(space(12));
        root.addView(statsPanel());
        root.addView(space(12));

        if (currentRole.equals(ROLE_HR)) {
            root.addView(managerForm());
            root.addView(space(12));
        }

        root.addView(notesToolbar());
        root.addView(space(8));
        notesContainer = new LinearLayout(this);
        notesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(notesContainer);
        renderNotesList();

        root.addView(space(16));
        Button back = outlineButton("الرجوع إلى الرئيسية");
        back.setOnClickListener(v -> showHome());
        root.addView(back);
    }

    private LinearLayout statsPanel() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        int today = notes.size();
        int waiting = 0;
        int reviewed = 0;
        for (ManagerNote n : notes) {
            if ("reviewed".equals(n.status)) reviewed++; else waiting++;
        }

        LinearLayout r1 = horizontal();
        r1.addView(statCard("حركات اليوم", String.valueOf(today), PRIMARY));
        r1.addView(spaceW(8));
        r1.addView(statCard("بانتظار المراجعة", String.valueOf(waiting), ORANGE));
        grid.addView(r1);
        grid.addView(space(8));
        LinearLayout r2 = horizontal();
        r2.addView(statCard("تمت مراجعتها", String.valueOf(reviewed), GREEN));
        r2.addView(spaceW(8));
        r2.addView(statCard("إجمالي السجل", String.valueOf(notes.size()), PURPLE));
        grid.addView(r2);
        return grid;
    }

    private LinearLayout managerForm() {
        LinearLayout form = card(18);
        form.setPadding(dp(16), dp(16), dp(16), dp(16));
        form.addView(text("إرسال ملاحظة جديدة", 18, TEXT, true));
        form.addView(space(8));
        form.addView(text("ابحث عن الموظف بكتابة حرفين أو أكثر. عند الاختيار تُجلب الشعبة الحالية من قاعدة البيانات.", 12, MUTED, false));
        form.addView(space(12));

        EditText search = editText("اكتب اسم الموظف للبحث...");
        search.setSingleLine(true);
        form.addView(search);
        form.addView(space(8));

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        form.addView(results);

        TextView selected = text(selectedEmployee == null ? "لم يتم اختيار موظف" : "الموظف المختار: " + selectedEmployee.name + "\nالشعبة الحالية: " + selectedEmployee.branch, 13, selectedEmployee == null ? MUTED : TEXT, false);
        selected.setPadding(dp(4), dp(8), dp(4), dp(8));
        form.addView(selected);

        LinearLayout chips = chipsBar();
        String[] movements = {"ملاحظة", "نقل", "تنسيب", "إنهاء تنسيب"};
        for (String m : movements) {
            Button b = chipButton(m, m.equals(currentMovement));
            b.setOnClickListener(v -> {
                currentMovement = ((Button) v).getText().toString();
                showManagerNotes();
            });
            chips.addView(b);
            chips.addView(spaceW(7));
        }
        form.addView(chips);
        form.addView(space(10));

        EditText note = editText("اكتب تفاصيل الملاحظة...");
        note.setMinLines(3);
        note.setGravity(Gravity.RIGHT | Gravity.TOP);
        form.addView(note);
        form.addView(space(12));

        Button send = primaryButton("إرسال الملاحظة");
        send.setOnClickListener(v -> {
            if (selectedEmployee == null) {
                Toast.makeText(this, "اختر الموظف أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            String noteText = note.getText().toString().trim();
            notes.add(0, new ManagerNote("MN-" + System.currentTimeMillis(), currentMovement, selectedEmployee.name,
                    selectedEmployee.branch, currentMovement.equals("ملاحظة") ? "" : "الشعبة الجديدة", noteText, "new", now()));
            saveNotesCache();
            Toast.makeText(this, "تم حفظ الملاحظة محليًا", Toast.LENGTH_SHORT).show();
            selectedEmployee = null;
            showManagerNotes();
        });
        form.addView(send);

        search.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && search.getText().toString().trim().length() < 2) {
                results.removeAllViews();
                results.addView(helperLine("اكتب حرفين أو أكثر لعرض النتائج"));
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = normalize(s.toString());
                results.removeAllViews();
                if (q.length() < 2) {
                    results.addView(helperLine("اكتب حرفين أو أكثر لعرض النتائج"));
                    return;
                }
                int hits = 0;
                for (Employee e : employees) {
                    if (employeeMatches(e, q)) {
                        hits++;
                        if (hits <= 12) {
                            TextView item = resultItem(e.name + "\n" + e.branch + " — " + e.typeLabel());
                            item.setOnClickListener(v -> {
                                selectedEmployee = e;
                                hideKeyboard(search);
                                search.clearFocus();
                                search.setText(e.name);
                                search.setSelection(search.getText().length());
                                results.removeAllViews();
                                selected.setText("الموظف المختار: " + e.name + "\nالشعبة الحالية: " + e.branch);
                                selected.setTextColor(TEXT);
                            });
                            results.addView(item);
                            results.addView(space(6));
                        }
                    }
                }
                if (hits == 0) results.addView(helperLine("لا توجد نتائج مطابقة"));
                if (hits > 12) results.addView(helperLine("تم عرض أول 12 نتيجة من أصل " + hits));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return form;
    }

    private LinearLayout notesToolbar() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.addView(text("سجل الملاحظات", 18, TEXT, true));
        box.addView(space(4));
        box.addView(text("يتم حفظ الملاحظات محليًا، مع تنظيم العرض حسب نوع الجهاز والصلاحيات المحددة في R2.0.6.", 11, MUTED, false));
        box.addView(space(8));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = chipsBar();
        String[] filters = {"الكل", "الجديد", "نقل", "تنسيب", "إنهاء تنسيب", "ملاحظات", "الأرشيف"};
        for (String f : filters) {
            Button b = chipButton(f, f.equals(currentFilter));
            b.setOnClickListener(v -> {
                currentFilter = ((Button) v).getText().toString();
                renderNotesList();
            });
            chips.addView(b);
            chips.addView(spaceW(7));
        }
        hsv.addView(chips);
        box.addView(hsv);
        return box;
    }

    private void renderNotesList() {
        if (notesContainer == null) return;
        notesContainer.removeAllViews();
        int shown = 0;
        for (ManagerNote n : notes) {
            if (!matchesFilter(n)) continue;
            if (currentRole.equals(ROLE_HR) && "reviewed".equals(n.status)) continue;
            notesContainer.addView(noteCard(n));
            notesContainer.addView(space(10));
            shown++;
        }
        if (shown == 0) notesContainer.addView(emptyCard("لا توجد ملاحظات ضمن هذا الفلتر"));
    }

    private boolean matchesFilter(ManagerNote n) {
        if ("الكل".equals(currentFilter)) return true;
        if ("الجديد".equals(currentFilter)) return "new".equals(n.status);
        if ("الأرشيف".equals(currentFilter)) return "reviewed".equals(n.status);
        if ("ملاحظات".equals(currentFilter)) return "ملاحظة".equals(n.type);
        return currentFilter.equals(n.type);
    }

    private LinearLayout noteCard(ManagerNote n) {
        LinearLayout c = card(18);
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout top = horizontal();
        top.addView(badge(n.type, movementColor(n.type)));
        top.addView(spaceW(8));
        top.addView(badge("new".equals(n.status) ? "جديد" : "تمت المراجعة", "new".equals(n.status) ? ORANGE : GREEN));
        c.addView(top);
        c.addView(space(8));
        c.addView(text(n.employee, 17, TEXT, true));
        c.addView(space(6));
        c.addView(text("من: " + safe(n.fromBranch) + (n.toBranch.length() > 0 ? "\nإلى: " + n.toBranch : ""), 13, MUTED, false));
        if (n.note.length() > 0) {
            c.addView(space(8));
            c.addView(text(n.note, 14, TEXT, false));
        }
        c.addView(space(8));
        c.addView(text("مرسلة من: مدير الموارد البشرية — " + n.date, 12, MUTED, false));
        if (currentRole.equals(ROLE_ADMIN) && "new".equals(n.status)) {
            c.addView(space(12));
            Button reviewed = primaryButton("تمت المراجعة");
            reviewed.setOnClickListener(v -> {
                n.status = "reviewed";
                saveNotesCache();
                Toast.makeText(this, "تمت المراجعة وحُفظت محليًا", Toast.LENGTH_SHORT).show();
                showManagerNotes();
            });
            c.addView(reviewed);
        }
        return c;
    }


    private void loadCachedNotes() {
        try {
            File f = new File(getFilesDir(), NOTES_CACHE_FILE);
            if (!f.exists()) return;
            byte[] bytes = new byte[(int) f.length()];
            try (FileInputStream fis = new FileInputStream(f)) {
                int read = fis.read(bytes);
                if (read <= 0) return;
            }
            JSONArray arr = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
            notes.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String id = o.optString("id", "MN-" + i);
                String type = o.optString("type", "ملاحظة");
                String employee = o.optString("employee", "");
                if (employee.length() == 0) continue;
                String fromBranch = o.optString("fromBranch", "");
                String toBranch = o.optString("toBranch", "");
                String note = o.optString("note", "");
                String status = o.optString("status", "new");
                String date = o.optString("date", now());
                notes.add(new ManagerNote(id, type, employee, fromBranch, toBranch, note, status, date));
            }
        } catch (Exception ignored) {}
    }

    private void saveNotesCache() {
        try {
            JSONArray arr = new JSONArray();
            for (ManagerNote n : notes) {
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("type", n.type);
                o.put("employee", n.employee);
                o.put("fromBranch", n.fromBranch);
                o.put("toBranch", n.toBranch);
                o.put("note", n.note);
                o.put("status", n.status);
                o.put("date", n.date);
                arr.put(o);
            }
            File f = new File(getFilesDir(), NOTES_CACHE_FILE);
            try (FileOutputStream fos = new FileOutputStream(f, false)) {
                fos.write(arr.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    private void syncEmployees(boolean returnToCurrentScreen) {
        if (isSyncing) return;
        isSyncing = true;
        Toast.makeText(this, "جاري تحميل بيانات الموظفين من GitHub...", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            try {
                String json = downloadText(DATA_URL);
                ParseResult parsed = parseEmployeesJson(json);
                saveCache(json);
                ui.post(() -> {
                    applyParseResult(parsed);
                    isSyncing = false;
                    Toast.makeText(this, "تم تحديث البيانات: " + employees.size() + " موظف", Toast.LENGTH_LONG).show();
                    showHome();
                });
            } catch (Exception ex) {
                ui.post(() -> {
                    isSyncing = false;
                    Toast.makeText(this, "فشل التحديث: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    showStatus();
                });
            }
        });
    }

    private String downloadText(String urlText) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlText);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Accept", "application/json,text/plain,*/*");
            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String text = readStream(in);
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code + " - " + text);
            return text;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String readStream(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private void loadCachedEmployees() {
        try {
            File f = new File(getFilesDir(), CACHE_FILE);
            if (!f.exists()) return;
            byte[] bytes = new byte[(int) f.length()];
            try (FileInputStream fis = new FileInputStream(f)) {
                int read = fis.read(bytes);
                if (read <= 0) return;
            }
            ParseResult parsed = parseEmployeesJson(new String(bytes, StandardCharsets.UTF_8));
            applyParseResult(parsed);
        } catch (Exception ignored) {}
    }

    private void saveCache(String json) throws Exception {
        File f = new File(getFilesDir(), CACHE_FILE);
        try (FileOutputStream fos = new FileOutputStream(f, false)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private ParseResult parseEmployeesJson(String raw) throws Exception {
        JSONObject obj = new JSONObject(raw);
        ParseResult out = new ParseResult();
        JSONObject meta = obj.optJSONObject("meta");
        if (meta != null) {
            out.version = meta.optString("version", "GitHub data");
            out.updatedAt = meta.optString("updatedAt", "");
            JSONObject counts = meta.optJSONObject("counts");
            if (counts != null) {
                out.permCount = counts.optInt("perm", 0);
                out.contCount = counts.optInt("cont", 0);
            }
        }
        readEmployeeArray(obj.optJSONArray("perm"), out.employees, "دائم");
        readEmployeeArray(obj.optJSONArray("cont"), out.employees, "عقد");
        if (out.permCount == 0 || out.contCount == 0) {
            int p = 0, c = 0;
            for (Employee e : out.employees) {
                if ("عقد".equals(e.typeLabel())) c++; else p++;
            }
            out.permCount = p;
            out.contCount = c;
        }
        if (out.employees.isEmpty()) throw new Exception("لم يتم العثور على بيانات الموظفين داخل JSON");
        out.lastSync = now();
        return out;
    }

    private void readEmployeeArray(JSONArray arr, List<Employee> target, String fallbackType) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String id = first(o, "employeeNo", "id", "integrationId", "sourceEmployeeId");
            String name = first(o, "name", "fullName", "employeeName");
            if (name.length() == 0) continue;
            String division = first(o, "division", "branch", "department");
            String status = first(o, "employmentStatus", "status", "type");
            String type = first(o, "type", "employmentStatus");
            if (type.length() == 0) type = fallbackType;
            String jobTitle = first(o, "jobTitle", "title");
            String grade = first(o, "grade");
            String step = first(o, "step");
            String salary = first(o, "salary");
            String education = first(o, "education");
            String mother = first(o, "motherName");
            String identity = first(o, "identityNo");
            String hireDate = first(o, "hireDate");
            String gender = first(o, "gender");
            String identityIssueDate = first(o, "identityIssueDate");
            String identityIssuer = first(o, "identityIssuer");
            String specialization = first(o, "specialization");
            String birthDate = first(o, "birthDate");
            String notes = first(o, "notes");
            String sourceUpdatedAt = first(o, "sourceUpdatedAt");
            target.add(new Employee(id, name, division, status.length() == 0 ? type : status, jobTitle, grade, step, salary, education, mother, identity, hireDate,
                    gender, identityIssueDate, identityIssuer, specialization, birthDate, notes, sourceUpdatedAt));
        }
    }

    private String first(JSONObject o, String... keys) {
        for (String k : keys) {
            Object v = o.opt(k);
            if (v != null && !JSONObject.NULL.equals(v)) {
                String s = String.valueOf(v).trim();
                if (s.length() > 0) return s;
            }
        }
        return "";
    }

    private void applyParseResult(ParseResult parsed) {
        employees.clear();
        employees.addAll(parsed.employees);
        permCount = parsed.permCount;
        contCount = parsed.contCount;
        dataVersion = parsed.version;
        lastSync = parsed.updatedAt.length() > 0 ? parsed.updatedAt : parsed.lastSync;
    }

    private boolean employeeMatches(Employee e, String normalizedQuery) {
        return normalize(e.name).contains(normalizedQuery)
                || normalize(e.id).contains(normalizedQuery)
                || normalize(e.branch).contains(normalizedQuery)
                || normalize(e.jobTitle).contains(normalizedQuery)
                || normalize(e.motherName).contains(normalizedQuery);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim()
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ى', 'ي')
                .replace('ة', 'ه')
                .replace("ـ", "")
                .toLowerCase(Locale.ROOT);
    }

    private Button roleButton(String label, String role) {
        Button b = pillButton(label, currentRole.equals(role) ? PRIMARY : Color.WHITE, currentRole.equals(role) ? Color.WHITE : TEXT);
        b.setOnClickListener(v -> {
            saveDeviceRole(role);
            Toast.makeText(this, "تم تثبيت نوع الجهاز: " + label, Toast.LENGTH_SHORT).show();
            showHome();
        });
        return b;
    }

    private LinearLayout dashboardCard(String title, String desc, int accent) {
        LinearLayout c = card(18);
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        c.setMinimumHeight(dp(112));
        c.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c.addView(badge("●", accent));
        c.addView(space(8));
        c.addView(text(title, 17, TEXT, true));
        c.addView(space(5));
        c.addView(text(desc, 12, MUTED, false));
        if ("ملاحظات المدير".equals(title)) c.setOnClickListener(v -> showManagerNotes());
        if ("القائمة".equals(title)) c.setOnClickListener(v -> showEmployeeDirectory());
        if ("حالة النظام".equals(title)) c.setOnClickListener(v -> showStatus());
        if ("التقارير".equals(title)) c.setOnClickListener(v -> Toast.makeText(this, "التقارير ستضاف في إصدار لاحق بعد تثبيت ملاحظات المدير", Toast.LENGTH_SHORT).show());
        return c;
    }

    private LinearLayout statCard(String label, String value, int accent) {
        LinearLayout c = card(16);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView v = text(value, 24, accent, true);
        TextView l = text(label, 12, MUTED, false);
        v.setGravity(Gravity.CENTER);
        l.setGravity(Gravity.CENTER);
        c.addView(v);
        c.addView(l);
        return c;
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 18, TEXT, true);
        t.setPadding(dp(2), dp(4), dp(2), dp(8));
        return t;
    }

    private LinearLayout card(int radius) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(round(CARD, radius, BORDER));
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        l.setLayoutParams(lp);
        return l;
    }

    private LinearLayout emptyCard(String message) {
        LinearLayout empty = card(18);
        empty.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView t = text(message, 14, MUTED, false);
        t.setGravity(Gravity.CENTER);
        empty.addView(t);
        return empty;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private EditText editText(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(14);
        e.setSingleLine(false);
        e.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackground(round(Color.WHITE, 14, BORDER));
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        return e;
    }

    private TextView helperLine(String s) {
        TextView t = text(s, 12, MUTED, false);
        t.setPadding(dp(8), dp(6), dp(8), dp(6));
        return t;
    }

    private TextView resultItem(String s) {
        TextView t = text(s, 14, TEXT, false);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        t.setBackground(round(Color.rgb(248, 250, 255), 14, BORDER));
        return t;
    }

    private Button primaryButton(String s) { return pillButton(s, PRIMARY, Color.WHITE); }
    private Button outlineButton(String s) { return pillButton(s, Color.WHITE, PRIMARY); }
    private Button chipButton(String s, boolean active) { return pillButton(s, active ? PRIMARY : Color.WHITE, active ? Color.WHITE : TEXT); }

    private Button pillButton(String s, int bg, int fg) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(fg);
        b.setTextSize(13);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(round(bg, 18, bg == Color.WHITE ? BORDER : bg));
        return b;
    }

    private TextView badge(String s, int color) {
        TextView b = text(s, 12, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(4), dp(10), dp(4));
        b.setBackground(round(color, 50, color));
        return b;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.RIGHT);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    private LinearLayout chipsBar() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.RIGHT);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private View spaceW(int w) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(w), 1));
        return v;
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private int movementColor(String type) {
        if ("نقل".equals(type)) return PURPLE;
        if ("تنسيب".equals(type)) return PRIMARY;
        if ("إنهاء تنسيب".equals(type)) return RED;
        return GREEN;
    }

    private String roleLabel() { return currentRole.equals(ROLE_ADMIN) ? "مسؤول النظام" : "مدير الموارد البشرية"; }
    private String safe(String v) { return v == null || v.length() == 0 ? "-" : v; }
    private String shortDate(String v) { return v == null || v.length() < 10 ? safe(v) : v.substring(0, 10); }

    private void hideKeyboard(View v) {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } catch (Exception ignored) {}
    }

    private String now() {
        return new SimpleDateFormat("yyyy/MM/dd - hh:mm a", new Locale("ar", "IQ")).format(new Date());
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    static class ParseResult {
        final List<Employee> employees = new ArrayList<>();
        String version = "GitHub data";
        String updatedAt = "";
        String lastSync = "";
        int permCount = 0;
        int contCount = 0;
    }

    static class Employee {
        final String id;
        final String name;
        final String branch;
        final String type;
        final String jobTitle;
        final String grade;
        final String step;
        final String salary;
        final String education;
        final String motherName;
        final String identityNo;
        final String hireDate;
        final String gender;
        final String identityIssueDate;
        final String identityIssuer;
        final String specialization;
        final String birthDate;
        final String notes;
        final String sourceUpdatedAt;

        Employee(String id, String name, String branch, String type, String jobTitle, String grade, String step, String salary, String education, String motherName, String identityNo, String hireDate) {
            this(id, name, branch, type, jobTitle, grade, step, salary, education, motherName, identityNo, hireDate,
                    "", "", "", "", "", "", "");
        }

        Employee(String id, String name, String branch, String type, String jobTitle, String grade, String step, String salary, String education, String motherName, String identityNo, String hireDate,
                 String gender, String identityIssueDate, String identityIssuer, String specialization, String birthDate, String notes, String sourceUpdatedAt) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.branch = branch == null ? "" : branch;
            this.type = type == null ? "" : type;
            this.jobTitle = jobTitle == null ? "" : jobTitle;
            this.grade = grade == null ? "" : grade;
            this.step = step == null ? "" : step;
            this.salary = salary == null ? "" : salary;
            this.education = education == null ? "" : education;
            this.motherName = motherName == null ? "" : motherName;
            this.identityNo = identityNo == null ? "" : identityNo;
            this.hireDate = hireDate == null ? "" : hireDate;
            this.gender = gender == null ? "" : gender;
            this.identityIssueDate = identityIssueDate == null ? "" : identityIssueDate;
            this.identityIssuer = identityIssuer == null ? "" : identityIssuer;
            this.specialization = specialization == null ? "" : specialization;
            this.birthDate = birthDate == null ? "" : birthDate;
            this.notes = notes == null ? "" : notes;
            this.sourceUpdatedAt = sourceUpdatedAt == null ? "" : sourceUpdatedAt;
        }
        String typeLabel() {
            String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
            if (t.contains("cont") || type.contains("عقد")) return "عقد";
            return "دائم";
        }
    }

    static class ManagerNote {
        final String id;
        final String type;
        final String employee;
        final String fromBranch;
        final String toBranch;
        final String note;
        String status;
        final String date;
        ManagerNote(String id, String type, String employee, String fromBranch, String toBranch, String note, String status, String date) {
            this.id = id;
            this.type = type;
            this.employee = employee;
            this.fromBranch = fromBranch == null ? "" : fromBranch;
            this.toBranch = toBranch == null ? "" : toBranch;
            this.note = note == null ? "" : note;
            this.status = status;
            this.date = date;
        }
    }
}
