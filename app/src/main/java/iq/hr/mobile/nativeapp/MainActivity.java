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
import android.widget.ImageView;
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

    private static final String APP_VERSION = "R2.5.0";
    private static final String DATA_URL = "https://raw.githubusercontent.com/muayedhassan/employees/main/data/employees.json";
    private static final String CACHE_FILE = "employees_cache_r2.json";
    private static final String NOTES_CACHE_FILE = "manager_notes_cache_r2.json";
    private static final String PREFS_FILE = "hr_native_preferences_r2";
    private static final String ROLE_ADMIN = "system_admin";
    private static final String ROLE_HR = "hr_manager";

    private static final int BG = Color.rgb(7, 13, 42);
    private static final int CARD = Color.rgb(11, 20, 55);
    private static final int TEXT = Color.rgb(232, 238, 255);
    private static final int MUTED = Color.rgb(136, 153, 204);
    private static final int PRIMARY = Color.rgb(92, 107, 192);
    private static final int GOLD = Color.rgb(245, 200, 66);
    private static final int GREEN = Color.rgb(102, 187, 106);
    private static final int ORANGE = Color.rgb(255, 167, 38);
    private static final int PURPLE = Color.rgb(186, 104, 200);
    private static final int RED = Color.rgb(239, 83, 80);
    private static final int BORDER = Color.rgb(37, 49, 88);
    private static final int NAVY = Color.rgb(16, 27, 78);
    private static final int SOFT_BLUE = Color.rgb(15, 26, 74);
    private static final int SOFT_GOLD = Color.rgb(44, 38, 15);

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
    private Typeface titleTypeface;
    private Typeface bodyTypeface;
    private Typeface numberTypeface;
    private String employeeDirectoryFilter = "الكل";
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadAppTypefaces();
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

    private void loadAppTypefaces() {
        try {
            titleTypeface = Typeface.createFromAsset(getAssets(), "fonts/YaModernPro-Bold.otf");
        } catch (Exception ignored) {
            titleTypeface = Typeface.DEFAULT_BOLD;
        }
        try {
            bodyTypeface = Typeface.createFromAsset(getAssets(), "fonts/SFSultan-Black.ttf");
        } catch (Exception ignored) {
            try {
                bodyTypeface = Typeface.createFromAsset(getAssets(), "fonts/ZainMobile.ttf");
            } catch (Exception ignored2) {
                bodyTypeface = Typeface.DEFAULT;
            }
        }
        try {
            numberTypeface = Typeface.createFromAsset(getAssets(), "fonts/Stencil.ttf");
        } catch (Exception ignored) {
            numberTypeface = Typeface.MONOSPACE;
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
        root.setPadding(dp(12), dp(14), dp(12), dp(26));
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

        root.addView(commandHero());

        root.addView(space(9));
        root.addView(webModeTabs());

        root.addView(space(9));
        root.addView(commandStatusPanel());

        root.addView(space(11));
        root.addView(sectionTitle("مركز العمليات"));
        root.addView(commandModuleGrid());

        root.addView(space(10));
        root.addView(homeActionBar());

        root.addView(space(10));
        TextView footer = text("R2.5.0 Premium Compact: كروت أصغر، تدرجات، ظلال، وأحجام خط أكثر هدوءًا.", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
    }

    private LinearLayout commandHero() {
        LinearLayout hero = card(22);
        hero.setPadding(dp(14), dp(14), dp(14), dp(14));
        hero.setBackground(gradient(Color.rgb(13, 25, 78), Color.rgb(29, 43, 118), 22, Color.rgb(58, 72, 135)));

        LinearLayout top = horizontal();
        ImageView logo = iconView(R.drawable.ic_hr_people, GOLD, Color.rgb(31, 43, 104), dp(48));
        top.addView(logo);
        top.addView(spaceW(12));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.RIGHT);
        titles.addView(text("سجل الموظفين", 22, Color.WHITE, true));
        titles.addView(space(3));
        titles.addView(text("مديرية زراعة صلاح الدين · Android Native", 11, Color.rgb(207, 226, 244), false));
        titles.addView(space(3));
        titles.addView(text(APP_VERSION + " Premium Compact", 11, GOLD, true));
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(top);

        hero.addView(space(11));
        LinearLayout roleBar = horizontal();
        roleBar.addView(lightBadge(roleLabel(), currentRole.equals(ROLE_ADMIN) ? GOLD : PURPLE));
        roleBar.addView(spaceW(8));
        roleBar.addView(lightBadge("توقيع ثابت", Color.rgb(129, 199, 132)));
        roleBar.addView(spaceW(8));
        roleBar.addView(lightBadge("Native UI", Color.rgb(125, 211, 252)));
        hero.addView(roleBar);

        hero.addView(space(11));
        LinearLayout kpis = horizontal();
        kpis.addView(heroKpi("الموظفون", String.valueOf(employees.size()), GOLD));
        kpis.addView(spaceW(8));
        kpis.addView(heroKpi("الدائميون", String.valueOf(permCount), GREEN));
        kpis.addView(spaceW(8));
        kpis.addView(heroKpi("العقود", String.valueOf(contCount), ORANGE));
        hero.addView(kpis);
        return hero;
    }

    private LinearLayout heroKpi(String label, String value, int accent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(7), dp(8), dp(7));
        box.setBackground(gradient(Color.rgb(18, 31, 86), Color.rgb(12, 22, 62), 14, Color.rgb(45, 60, 120)));
        box.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView v = text(value, 21, accent, true);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        TextView l = text(label, 9, Color.rgb(190, 205, 240), false);
        l.setGravity(Gravity.CENTER);
        box.addView(v);
        box.addView(l);
        return box;
    }

    private LinearLayout commandStatusPanel() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(11), dp(12), dp(11));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(9, 18, 52), 18, BORDER));
        LinearLayout top = horizontal();
        top.addView(iconView(R.drawable.ic_hr_sync, Color.rgb(125, 211, 252), Color.rgb(12, 35, 74), dp(40)));
        top.addView(spaceW(9));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(text("حالة البيانات والتزامن", 15, TEXT, true));
        t.addView(space(3));
        t.addView(text("آخر تحديث: " + lastSync, 10, MUTED, false));
        t.addView(text("نسخة البيانات: " + dataVersion, 10, MUTED, false));
        top.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(top);
        box.addView(space(9));
        LinearLayout mini = horizontal();
        mini.addView(compactPill("ملاحظات جديدة", String.valueOf(countNewNotes()), RED));
        mini.addView(spaceW(8));
        mini.addView(compactPill("مراجعة", String.valueOf(countReviewedNotes()), GREEN));
        mini.addView(spaceW(8));
        mini.addView(compactPill("نطاق", employeeDirectoryFilter, PRIMARY));
        box.addView(mini);
        return box;
    }

    private LinearLayout compactPill(String label, String value, int accent) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(7), dp(6), dp(7), dp(6));
        p.setBackground(round(Color.rgb(16, 27, 78), 12, Color.rgb(38, 52, 100)));
        TextView v = text(value, hasDigit(value) ? 16 : 11, accent, true);
        v.setGravity(Gravity.CENTER);
        if (hasDigit(value)) v.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        TextView l = text(label, 8, MUTED, false);
        l.setGravity(Gravity.CENTER);
        p.addView(v);
        p.addView(l);
        p.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return p;
    }

    private LinearLayout commandModuleGrid() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout r1 = horizontal();
        r1.addView(moduleCard("قائمة الموظفين", "بحث إداري، فلترة، وبطاقة موظف كاملة", R.drawable.ic_hr_search, GOLD, v -> {
            employeeDirectoryFilter = "الكل";
            showEmployeeDirectory();
        }));
        r1.addView(spaceW(8));
        r1.addView(moduleCard("ملاحظات المدير", "حركات إدارية وسجل مراجعة حسب نوع الجهاز", R.drawable.ic_hr_notes, PURPLE, v -> showManagerNotes()));
        grid.addView(r1);

        grid.addView(space(8));
        LinearLayout r2 = horizontal();
        r2.addView(moduleCard("لوحة المسؤول", "مؤشرات تشغيل ومراجعة سريعة", R.drawable.ic_hr_admin, GREEN, v -> showAdminDashboard()));
        r2.addView(spaceW(8));
        r2.addView(moduleCard("جودة البيانات", "اكتمال ملفات ونواقص داخل بطاقة الموظف", R.drawable.ic_hr_quality, ORANGE, v -> {
            employeeDirectoryFilter = "الكل";
            showEmployeeDirectory();
        }));
        grid.addView(r2);

        grid.addView(space(8));
        LinearLayout r3 = horizontal();
        r3.addView(moduleCard("مركز التحديث", "آلية التثبيت واسم Artifact الحالي", R.drawable.ic_hr_update, Color.rgb(125, 211, 252), v -> showUpdateCenter()));
        r3.addView(spaceW(8));
        r3.addView(moduleCard("ملف الموظف", "هوية وظيفية بتقسيم قريب من الويب", R.drawable.ic_hr_profile, PRIMARY, v -> {
            employeeDirectoryFilter = "الكل";
            showEmployeeDirectory();
        }));
        grid.addView(r3);
        return grid;
    }

    private LinearLayout moduleCard(String title, String desc, int iconRes, int accent, View.OnClickListener listener) {
        LinearLayout c = card(16);
        c.setPadding(dp(10), dp(10), dp(10), dp(10));
        c.setMinimumHeight(dp(106));
        c.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(9, 18, 52), 16, Color.rgb(38, 52, 100)));
        c.setOnClickListener(listener);

        LinearLayout top = horizontal();
        top.addView(iconView(iconRes, accent, Color.rgb(22, 34, 83), dp(38)));
        top.addView(spaceW(8));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text(title, 14, TEXT, true));
        titleBox.addView(space(3));
        titleBox.addView(text(desc, 9, MUTED, false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c.addView(top);

        c.addView(space(8));
        View line = new View(this);
        line.setBackgroundColor(accent);
        c.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        c.addView(space(6));
        c.addView(text("فتح", 10, accent, true));
        return c;
    }

    private LinearLayout homeActionBar() {
        LinearLayout actions = horizontal();
        Button syncBtn = primaryButton(isSyncing ? "جاري التحديث..." : "تحديث البيانات");
        syncBtn.setEnabled(!isSyncing);
        syncBtn.setOnClickListener(v -> syncEmployees(true));
        actions.addView(syncBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(spaceW(8));
        Button role = outlineButton("نوع الجهاز");
        role.setOnClickListener(v -> showDeviceRoleSetup());
        actions.addView(role, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return actions;
    }

    private LinearLayout webModeTabs() {
        LinearLayout box = card(16);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackground(gradient(Color.rgb(12, 22, 58), Color.rgb(8, 16, 44), 16, BORDER));
        box.addView(text("نطاق السجل", 14, TEXT, true));
        box.addView(space(6));
        LinearLayout tabs = horizontal();
        Button perm = primaryButton("الدائميون");
        perm.setOnClickListener(v -> {
            employeeDirectoryFilter = "دائم";
            showEmployeeDirectory();
        });
        tabs.addView(perm, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tabs.addView(spaceW(6));
        Button cont = outlineButton("العقود");
        cont.setOnClickListener(v -> {
            employeeDirectoryFilter = "عقد";
            showEmployeeDirectory();
        });
        tabs.addView(cont, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tabs.addView(spaceW(6));
        Button admin = outlineButton("الإدارة");
        admin.setOnClickListener(v -> showAdminDashboard());
        tabs.addView(admin, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(tabs);
        return box;
    }

    private LinearLayout executiveHeader() {
        LinearLayout hero = card(24);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(round(NAVY, 24, PRIMARY));

        LinearLayout top = horizontal();
        TextView logo = text("HR", 22, NAVY, true);
        logo.setGravity(Gravity.CENTER);
        logo.setPadding(dp(13), dp(9), dp(13), dp(9));
        logo.setBackground(round(GOLD, 18, GOLD));
        top.addView(logo);
        top.addView(spaceW(10));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        titles.addView(text("نظام الموارد البشرية", 25, Color.WHITE, true));
        titles.addView(space(4));
        titles.addView(text("واجهة Native احترافية — " + APP_VERSION, 13, Color.rgb(207, 226, 244), false));
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(top);

        hero.addView(space(14));
        LinearLayout roleBar = horizontal();
        roleBar.addView(lightBadge(roleLabel(), currentRole.equals(ROLE_ADMIN) ? GOLD : Color.rgb(206, 147, 216)));
        roleBar.addView(spaceW(8));
        roleBar.addView(lightBadge("بيانات: " + employees.size(), Color.rgb(125, 211, 252)));
        roleBar.addView(spaceW(8));
        roleBar.addView(lightBadge("ملاحظات: " + notes.size(), Color.rgb(105, 240, 174)));
        hero.addView(roleBar);

        hero.addView(space(14));
        TextView summary = text("آخر تحديث بيانات: " + lastSync + "\nنسخة البيانات: " + dataVersion, 12, Color.rgb(238, 246, 255), false);
        summary.setLineSpacing(dp(2), 1.0f);
        hero.addView(summary);
        return hero;
    }

    private LinearLayout screenHero(String titleText, String subtitleText, int iconRes, int accent) {
        LinearLayout header = card(18);
        header.setPadding(dp(13), dp(13), dp(13), dp(13));
        header.setBackground(gradient(Color.rgb(13, 25, 78), Color.rgb(8, 17, 48), 18, Color.rgb(42, 56, 118)));
        LinearLayout top = horizontal();
        top.addView(iconView(iconRes, accent, Color.rgb(22, 34, 83), dp(42)));
        top.addView(spaceW(10));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(titleText, 18, Color.WHITE, true));
        labels.addView(space(3));
        labels.addView(text(subtitleText, 10, Color.rgb(190, 205, 240), false));
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(top);
        header.addView(space(9));
        View line = new View(this);
        line.setBackgroundColor(accent);
        header.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        return header;
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

    private LinearLayout executiveStatsPanel() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout r1 = horizontal();
        r1.addView(statCard("إجمالي الموظفين", String.valueOf(employees.size()), PRIMARY));
        r1.addView(spaceW(8));
        r1.addView(statCard("الملاك الدائم", String.valueOf(permCount), GREEN));
        grid.addView(r1);
        grid.addView(space(8));
        LinearLayout r2 = horizontal();
        r2.addView(statCard("العقود", String.valueOf(contCount), ORANGE));
        r2.addView(spaceW(8));
        r2.addView(statCard("بانتظار المراجعة", String.valueOf(countNewNotes()), RED));
        grid.addView(r2);
        return grid;
    }

    private LinearLayout workflowPanel() {
        LinearLayout box = card(18);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackground(round(SOFT_GOLD, 18, Color.rgb(255, 224, 138)));
        box.addView(text("مسار العمل الجديد", 18, TEXT, true));
        box.addView(space(8));
        box.addView(text("1. حدّث بيانات الموظفين من GitHub\n2. ابحث وافتح بطاقة الموظف\n3. أرسل الملاحظة أو راجعها حسب نوع الجهاز\n4. تابع الإصدار من مركز التحديث", 13, TEXT, false));
        box.addView(space(12));
        LinearLayout actions = horizontal();
        Button status = outlineButton("حالة النظام");
        status.setOnClickListener(v -> showStatus());
        actions.addView(status, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(spaceW(8));
        Button role = outlineButton("تغيير نوع الجهاز");
        role.setOnClickListener(v -> showDeviceRoleSetup());
        actions.addView(role, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(actions);
        return box;
    }

    private int countNewNotes() {
        int count = 0;
        for (ManagerNote n : notes) {
            if ("new".equals(n.status)) count++;
        }
        return count;
    }

    private int countReviewedNotes() {
        int count = 0;
        for (ManagerNote n : notes) {
            if ("reviewed".equals(n.status)) count++;
        }
        return count;
    }

    private void showEmployeeDirectory() {
        baseScreen();

        LinearLayout header = screenHero("سجل الموظفين", "بحث Native قريب من نسخة الويب مع فلاتر الدائميين والعقود", R.drawable.ic_hr_search, GOLD);
        header.addView(space(12));
        LinearLayout badges = horizontal();
        badges.addView(badge(filteredEmployeeCount() + " ضمن الفلتر", PRIMARY));
        badges.addView(spaceW(8));
        badges.addView(badge("دائم " + permCount, GREEN));
        badges.addView(spaceW(8));
        badges.addView(badge("عقود " + contCount, ORANGE));
        header.addView(badges);
        root.addView(header);

        root.addView(space(9));
        LinearLayout searchCard = card(16);
        searchCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        searchCard.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 16, BORDER));
        searchCard.addView(text("البحث الإداري السريع", 15, TEXT, true));
        searchCard.addView(space(4));
        searchCard.addView(text("النطاق الحالي: " + employeeDirectoryFilter + "، ابحث بالاسم أو الرقم أو الشعبة أو اسم الأم.", 10, MUTED, false));
        searchCard.addView(space(8));
        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filterChips = chipsBar();
        String[] employeeFilters = {"الكل", "دائم", "عقد"};
        for (String f : employeeFilters) {
            Button b = chipButton(f, f.equals(employeeDirectoryFilter));
            b.setOnClickListener(v -> {
                employeeDirectoryFilter = ((Button) v).getText().toString();
                showEmployeeDirectory();
            });
            filterChips.addView(b);
            filterChips.addView(spaceW(7));
        }
        filterScroll.addView(filterChips);
        searchCard.addView(filterScroll);
        searchCard.addView(space(8));
        EditText search = editText("اكتب اسم الموظف أو الرقم الوظيفي...");
        search.setSingleLine(true);
        searchCard.addView(search);
        searchCard.addView(space(8));
        TextView status = text("جاهز للبحث", 10, MUTED, false);
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
                results.addView(emptyCard("اكتب حرفين أو أكثر حتى تظهر النتائج ضمن نطاق " + employeeDirectoryFilter));
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
            container.addView(emptyCard("اكتب حرفين أو أكثر للبحث في " + filteredEmployeeCount() + " موظف ضمن نطاق " + employeeDirectoryFilter));
            status.setText("النطاق الجاهز: " + employeeDirectoryFilter + " — " + filteredEmployeeCount() + " موظف");
            return;
        }
        int shown = 0;
        int matched = 0;
        for (Employee e : employees) {
            if (employeePassesDirectoryFilter(e) && employeeMatches(e, q)) {
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
        status.setText("النتائج ضمن " + employeeDirectoryFilter + ": " + matched + (matched > 50 ? " — عُرضت أول 50 نتيجة فقط" : ""));
    }

    private boolean employeePassesDirectoryFilter(Employee e) {
        if ("دائم".equals(employeeDirectoryFilter)) return "دائم".equals(e.typeLabel());
        if ("عقد".equals(employeeDirectoryFilter)) return "عقد".equals(e.typeLabel());
        return true;
    }

    private int filteredEmployeeCount() {
        int count = 0;
        for (Employee e : employees) {
            if (employeePassesDirectoryFilter(e)) count++;
        }
        return count;
    }

    private LinearLayout employeeCard(Employee e) {
        LinearLayout c = card(16);
        c.setPadding(dp(10), dp(10), dp(10), dp(10));
        c.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 16, Color.rgb(42, 56, 118)));
        c.setOnClickListener(v -> showEmployeeProfile(e));

        LinearLayout top = horizontal();
        TextView avatar = text(cardInitials(e.name), 15, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(gradient("عقد".equals(e.typeLabel()) ? ORANGE : PRIMARY, Color.rgb(22, 34, 83), 15, "عقد".equals(e.typeLabel()) ? ORANGE : PRIMARY));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));
        top.addView(spaceW(8));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.RIGHT);
        titleBox.addView(text(e.name, 15, TEXT, true));
        titleBox.addView(space(3));
        titleBox.addView(text(safe(e.jobTitle) + " · " + safe(e.branch), 10, MUTED, false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c.addView(top);

        c.addView(space(8));
        LinearLayout badges = horizontal();
        badges.addView(miniBadge(e.typeLabel(), "عقد".equals(e.typeLabel()) ? ORANGE : GREEN));
        badges.addView(spaceW(6));
        badges.addView(miniBadge("رقم " + safe(e.id), PRIMARY));
        badges.addView(spaceW(6));
        badges.addView(miniBadge(profileCompletion(e) + "%", profileCompletion(e) >= 75 ? GREEN : ORANGE));
        c.addView(badges);
        c.addView(space(6));
        c.addView(text("الدرجة/المرحلة: " + safe(e.grade) + " / " + safe(e.step), 11, TEXT, false));
        c.addView(text("التحصيل: " + safe(e.education) + " · التعيين: " + shortDate(e.hireDate), 10, MUTED, false));
        c.addView(space(7));

        LinearLayout actions = horizontal();
        Button open = primaryButton("فتح بطاقة الموظف");
        open.setOnClickListener(v -> showEmployeeProfile(e));
        actions.addView(open, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(spaceW(6));
        Button choose = outlineButton("اختيار للملاحظات");
        choose.setOnClickListener(v -> {
            selectedEmployee = e;
            Toast.makeText(this, "تم اختيار الموظف للملاحظات", Toast.LENGTH_SHORT).show();
            showManagerNotes();
        });
        if (currentRole.equals(ROLE_HR)) {
            actions.addView(choose, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        } else {
            Button review = outlineButton("فتح المراجعة");
            review.setOnClickListener(v -> showManagerNotes());
            actions.addView(review, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        c.addView(actions);
        return c;
    }

    private void showEmployeeProfile(Employee e) {
        baseScreen();

        root.addView(employeeProfileHero(e));

        root.addView(space(12));
        LinearLayout metrics1 = horizontal();
        metrics1.addView(profileMetric("اكتمال الملف", profileCompletion(e) + "%", profileCompletion(e) >= 75 ? GREEN : ORANGE));
        metrics1.addView(spaceW(8));
        metrics1.addView(profileMetric("النواقص", String.valueOf(profileMissingCount(e)), profileMissingCount(e) == 0 ? GREEN : RED));
        root.addView(metrics1);
        root.addView(space(8));
        LinearLayout metrics2 = horizontal();
        metrics2.addView(profileMetric("الرقم الوظيفي", safe(e.id), PRIMARY));
        metrics2.addView(spaceW(8));
        metrics2.addView(profileMetric("نوع التوظيف", e.typeLabel(), "عقد".equals(e.typeLabel()) ? ORANGE : GREEN));
        root.addView(metrics2);

        root.addView(space(12));
        root.addView(dataQualityCard(e));

        root.addView(space(10));
        root.addView(infoSection("البيانات الأساسية",
                infoRow("الاسم الكامل", safe(e.name))
                        + infoRow("اسم الأم", safe(e.motherName))
                        + infoRow("الجنس", safe(e.gender))
                        + infoRow("تاريخ الولادة", shortDate(e.birthDate))
        ));

        root.addView(space(10));
        root.addView(infoSection("البيانات الوظيفية",
                infoRow("الشعبة", safe(e.branch))
                        + infoRow("العنوان الوظيفي", safe(e.jobTitle))
                        + infoRow("الحالة", safe(e.type))
                        + infoRow("الدرجة", safe(e.grade))
                        + infoRow("المرحلة", safe(e.step))
                        + infoRow("الراتب", formatSalary(e.salary))
                        + infoRow("تاريخ التعيين", shortDate(e.hireDate))
        ));

        root.addView(space(10));
        root.addView(infoSection("الهوية الشخصية",
                infoRow("رقم الهوية", safe(e.identityNo))
                        + infoRow("جهة الإصدار", safe(e.identityIssuer))
                        + infoRow("تاريخ الإصدار", shortDate(e.identityIssueDate))
        ));

        root.addView(space(10));
        root.addView(infoSection("معلومات إضافية",
                infoRow("التحصيل", safe(e.education))
                        + infoRow("الاختصاص", safe(e.specialization))
                        + infoRow("آخر تحديث للبيانات", shortDate(e.sourceUpdatedAt))
                        + infoRow("ملاحظات", safe(e.notes))
        ));

        root.addView(space(14));
        Button note = primaryButton(currentRole.equals(ROLE_HR) ? "اختيار الموظف في ملاحظات المدير" : "فتح ملاحظات المدير للمراجعة");
        note.setOnClickListener(v -> {
            if (currentRole.equals(ROLE_HR)) {
                selectedEmployee = e;
            }
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

    private LinearLayout employeeProfileHero(Employee e) {
        LinearLayout header = card(18);
        header.setPadding(dp(13), dp(13), dp(13), dp(13));
        header.setBackground(gradient(Color.rgb(13, 25, 78), Color.rgb(8, 17, 48), 18, NAVY));

        LinearLayout top = horizontal();
        TextView avatar = text(cardInitials(e.name), 18, NAVY, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(gradient(GOLD, Color.rgb(255, 167, 38), 18, GOLD));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(54), dp(54)));
        top.addView(spaceW(10));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.RIGHT);
        titleBox.addView(text(e.name, 19, Color.WHITE, true));
        titleBox.addView(space(3));
        titleBox.addView(text(safe(e.jobTitle) + " · " + safe(e.branch), 11, Color.rgb(207, 226, 244), false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(top);

        header.addView(space(10));
        LinearLayout badges = horizontal();
        badges.addView(lightBadge(e.typeLabel(), "عقد".equals(e.typeLabel()) ? ORANGE : GREEN));
        badges.addView(spaceW(6));
        badges.addView(lightBadge("رقم وظيفي: " + safe(e.id), SOFT_BLUE));
        badges.addView(spaceW(6));
        badges.addView(lightBadge("ملف ويب داخل Native", SOFT_GOLD));
        header.addView(badges);
        return header;
    }

    private LinearLayout profileMetric(String label, String value, int accent) {
        LinearLayout c = card(14);
        c.setPadding(dp(10), dp(9), dp(10), dp(9));
        c.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 14, BORDER));
        c.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView v = text(value, 18, accent, true);
        v.setGravity(Gravity.CENTER);
        if (hasDigit(value)) {
            v.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        } else {
            v.setTypeface(titleTypeface == null ? Typeface.DEFAULT_BOLD : titleTypeface, Typeface.BOLD);
        }
        TextView l = text(label, 9, MUTED, false);
        l.setGravity(Gravity.CENTER);
        c.addView(v);
        c.addView(l);
        return c;
    }

    private LinearLayout dataQualityCard(Employee e) {
        LinearLayout box = card(16);
        box.setPadding(dp(12), dp(11), dp(12), dp(11));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 16, BORDER));
        box.addView(text("مركز جودة البيانات", 15, TEXT, true));
        box.addView(space(6));
        int missing = profileMissingCount(e);
        String state = missing == 0 ? "الملف مكتمل في الحقول الأساسية المتوفرة." : "نواقص تحتاج مراجعة: " + missing;
        box.addView(text(state, 11, missing == 0 ? GREEN : RED, true));
        box.addView(space(6));
        box.addView(text(missingFieldsText(e), 11, TEXT, false));
        box.addView(space(6));
        box.addView(text("يعرض النواقص الأساسية مباشرة داخل بطاقة الموظف.", 10, MUTED, false));
        return box;
    }

    private int profileCompletion(Employee e) {
        String[] values = {
                e.id, e.name, e.branch, e.type, e.jobTitle, e.grade, e.step, e.salary,
                e.education, e.motherName, e.identityNo, e.hireDate, e.gender,
                e.identityIssueDate, e.identityIssuer, e.specialization, e.birthDate
        };
        int filled = 0;
        for (String value : values) {
            if (!isBlankValue(value)) filled++;
        }
        return Math.round((filled * 100f) / values.length);
    }

    private int profileMissingCount(Employee e) {
        return missingFieldLabels(e).size();
    }

    private String missingFieldsText(Employee e) {
        List<String> missing = missingFieldLabels(e);
        if (missing.isEmpty()) return "لا توجد نواقص أساسية ظاهرة حاليًا.";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) sb.append("، ");
            sb.append(missing.get(i));
        }
        return sb.toString();
    }

    private List<String> missingFieldLabels(Employee e) {
        List<String> missing = new ArrayList<>();
        if (isBlankValue(e.id)) missing.add("الرقم الوظيفي");
        if (isBlankValue(e.branch)) missing.add("الشعبة");
        if (isBlankValue(e.jobTitle)) missing.add("العنوان الوظيفي");
        if (isBlankValue(e.grade)) missing.add("الدرجة");
        if (isBlankValue(e.step)) missing.add("المرحلة");
        if (isBlankValue(e.education)) missing.add("التحصيل");
        if (isBlankValue(e.motherName)) missing.add("اسم الأم");
        if (isBlankValue(e.identityNo)) missing.add("رقم الهوية");
        if (isBlankValue(e.hireDate)) missing.add("تاريخ التعيين");
        return missing;
    }

    private boolean isBlankValue(String value) {
        if (value == null) return true;
        String v = value.trim();
        return v.length() == 0 || "-".equals(v) || "null".equalsIgnoreCase(v);
    }

    private boolean hasDigit(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) return true;
        }
        return false;
    }

    private String cardInitials(String name) {
        String clean = safe(name).replace("-", "").trim();
        if (clean.length() == 0) return "HR";
        String[] parts = clean.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.length() == 0) continue;
            out.append(part.charAt(0));
            if (out.length() == 2) break;
        }
        return out.length() == 0 ? "HR" : out.toString();
    }

    private LinearLayout infoSection(String title, String body) {
        LinearLayout box = card(14);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 14, BORDER));
        box.addView(text(title, 14, TEXT, true));
        box.addView(space(6));
        TextView content = text(body, 11, TEXT, false);
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
        LinearLayout header = screenHero("حالة النظام", "تشخيص نسخة Android Native ومزامنة بيانات الموظفين", R.drawable.ic_hr_sync, Color.rgb(125, 211, 252));
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
        Button update = outlineButton("فتح مركز التحديث");
        update.setOnClickListener(v -> showUpdateCenter());
        root.addView(update);
        root.addView(space(8));
        Button back = outlineButton("الرجوع إلى الرئيسية");
        back.setOnClickListener(v -> showHome());
        root.addView(back);
    }

    private void showAdminDashboard() {
        baseScreen();
        LinearLayout header = screenHero("لوحة مسؤول النظام", "مؤشرات تشغيل سريعة ومراجعة ملاحظات الموارد البشرية", R.drawable.ic_hr_admin, GREEN);
        header.addView(space(12));
        LinearLayout badges = horizontal();
        badges.addView(badge("جديد: " + countNewNotes(), RED));
        badges.addView(spaceW(8));
        badges.addView(badge("مؤرشف: " + countReviewedNotes(), GREEN));
        badges.addView(spaceW(8));
        badges.addView(badge(APP_VERSION, PRIMARY));
        header.addView(badges);
        root.addView(header);

        root.addView(space(12));
        root.addView(executiveStatsPanel());

        root.addView(space(12));
        LinearLayout reviewBox = card(18);
        reviewBox.setPadding(dp(16), dp(14), dp(16), dp(14));
        reviewBox.addView(text("مراجعة سريعة", 18, TEXT, true));
        reviewBox.addView(space(6));
        reviewBox.addView(text("هذه اللوحة تجمع أهم أرقام الموظفين والملاحظات، وتفتح سجل المراجعة مباشرة لمسؤول النظام.", 12, MUTED, false));
        reviewBox.addView(space(12));
        Button notesButton = primaryButton("فتح سجل الملاحظات والمراجعة");
        notesButton.setOnClickListener(v -> showManagerNotes());
        reviewBox.addView(notesButton);
        root.addView(reviewBox);

        root.addView(space(12));
        LinearLayout health = card(18);
        health.setPadding(dp(16), dp(14), dp(16), dp(14));
        health.addView(text("حالة البيانات", 18, TEXT, true));
        health.addView(space(6));
        health.addView(text("المصدر: GitHub JSON\nعدد الموظفين المحلي: " + employees.size() + "\nآخر تحديث: " + lastSync, 13, TEXT, false));
        health.addView(space(12));
        Button sync = outlineButton(isSyncing ? "جاري التحديث..." : "تحديث بيانات الموظفين");
        sync.setEnabled(!isSyncing);
        sync.setOnClickListener(v -> syncEmployees(true));
        health.addView(sync);
        root.addView(health);

        root.addView(space(12));
        Button back = outlineButton("الرجوع إلى الرئيسية");
        back.setOnClickListener(v -> showHome());
        root.addView(back);
    }

    private void showUpdateCenter() {
        baseScreen();
        LinearLayout header = screenHero("مركز تحديث التطبيق", "تعليمات ثابتة للتحديث من GitHub Actions بدون حذف التطبيق", R.drawable.ic_hr_update, ORANGE);
        header.addView(space(12));
        LinearLayout badges = horizontal();
        badges.addView(badge("الإصدار الحالي: " + APP_VERSION, PRIMARY));
        badges.addView(spaceW(8));
        badges.addView(badge("توقيع ثابت", GREEN));
        header.addView(badges);
        root.addView(header);

        root.addView(space(12));
        LinearLayout box = card(18);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.addView(text("آلية التحديث المعتمدة", 18, TEXT, true));
        box.addView(space(8));
        box.addView(text("1. فك ملف Patch zip فوق مشروع HRNativeAndroidR2\n2. نفذ git add -A ثم commit ثم push\n3. افتح GitHub Actions وانتظر نجاح البناء\n4. حمّل Artifact الناتج\n5. افتح app-release.apk وثبّته فوق النسخة السابقة", 13, TEXT, false));
        box.addView(space(12));
        box.addView(text("مهم: استخدم فقط APK الناتج من GitHub Actions لأن توقيعه ثابت، أما APK المحلي أو debug فقد لا يثبت فوق النسخة السابقة.", 12, RED, true));
        root.addView(box);

        root.addView(space(12));
        LinearLayout release = card(18);
        release.setPadding(dp(16), dp(14), dp(16), dp(14));
        release.addView(text("محتوى R2.5.0", 18, TEXT, true));
        release.addView(space(8));
        release.addView(text("• تحويل الواجهة إلى Premium Compact\n• كروت أصغر وأكثر كثافة وتنظيمًا\n• تدرجات وظلال Native للكروت والهيدرات\n• تحسين أحجام الخطوط والشارات والأزرار\n• تصغير كروت نتائج الموظفين مع إبقاء الوظائف\n• تحسين فقاعات الأيقونات حسب الحجم\n• استمرار خطوط SF Sultan وYa Modern Pro وStencil", 13, TEXT, false));
        root.addView(release);

        root.addView(space(12));
        Button status = primaryButton("فتح حالة النظام");
        status.setOnClickListener(v -> showStatus());
        root.addView(status);
        root.addView(space(8));
        Button back = outlineButton("الرجوع إلى الرئيسية");
        back.setOnClickListener(v -> showHome());
        root.addView(back);
    }

    private void showManagerNotes() {
        if (currentRole.equals(ROLE_HR) && "الأرشيف".equals(currentFilter)) {
            currentFilter = "الكل";
        }
        baseScreen();

        LinearLayout header = screenHero("ملاحظات مدير الموارد البشرية", "متابعة النقل، التنسيب، إنهاء التنسيب، والملاحظات الإدارية", R.drawable.ic_hr_notes, PURPLE);
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
        box.addView(text("يتم حفظ الملاحظات محليًا، مع تنظيم العرض حسب نوع الجهاز والصلاحيات المحددة في R2.5.0.", 11, MUTED, false));
        box.addView(space(8));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = chipsBar();
        String[] filters = currentRole.equals(ROLE_ADMIN)
                ? new String[]{"الكل", "الجديد", "نقل", "تنسيب", "إنهاء تنسيب", "ملاحظات", "الأرشيف"}
                : new String[]{"الكل", "الجديد", "نقل", "تنسيب", "إنهاء تنسيب", "ملاحظات"};
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
        if ("القائمة".equals(title)) c.setOnClickListener(v -> {
            employeeDirectoryFilter = "الكل";
            showEmployeeDirectory();
        });
        if ("حالة النظام".equals(title)) c.setOnClickListener(v -> showStatus());
        if ("لوحة المسؤول".equals(title)) c.setOnClickListener(v -> showAdminDashboard());
        if ("مركز التحديث".equals(title)) c.setOnClickListener(v -> showUpdateCenter());
        if ("التقارير".equals(title)) c.setOnClickListener(v -> Toast.makeText(this, "التقارير ستضاف في إصدار لاحق بعد تثبيت ملاحظات المدير", Toast.LENGTH_SHORT).show());
        return c;
    }

    private LinearLayout statCard(String label, String value, int accent) {
        LinearLayout c = card(16);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView v = text(value, 24, accent, true);
        TextView l = text(label, 12, MUTED, false);
        v.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        l.setGravity(Gravity.CENTER);
        c.addView(v);
        c.addView(l);
        return c;
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 15, TEXT, true);
        t.setPadding(dp(2), dp(3), dp(2), dp(6));
        return t;
    }

    private LinearLayout card(int radius) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(round(CARD, radius, BORDER));
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        l.setElevation(dp(2));
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
        if (bold) {
            t.setTypeface(titleTypeface == null ? Typeface.DEFAULT_BOLD : titleTypeface, Typeface.BOLD);
        } else {
            t.setTypeface(bodyTypeface == null ? Typeface.DEFAULT : bodyTypeface);
        }
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
        e.setBackground(round(Color.rgb(15, 26, 74), 14, BORDER));
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTypeface(bodyTypeface == null ? Typeface.DEFAULT : bodyTypeface);
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
    private Button outlineButton(String s) { return pillButton(s, Color.rgb(15, 26, 74), Color.rgb(207, 226, 244)); }
    private Button chipButton(String s, boolean active) { return pillButton(s, active ? PRIMARY : Color.rgb(15, 26, 74), active ? Color.WHITE : TEXT); }

    private Button pillButton(String s, int bg, int fg) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(fg);
        b.setTextSize(12);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackground(round(bg, 18, bg == Color.WHITE ? BORDER : Color.rgb(52, 66, 125)));
        b.setTypeface(titleTypeface == null ? Typeface.DEFAULT_BOLD : titleTypeface, Typeface.BOLD);
        return b;
    }

    private TextView badge(String s, int color) {
        TextView b = text(s, 10, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), dp(3), dp(8), dp(3));
        b.setBackground(round(color, 50, color));
        return b;
    }

    private TextView miniBadge(String s, int color) {
        TextView b = text(s, 10, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(7), dp(3), dp(7), dp(3));
        b.setBackground(gradient(color, Color.rgb(22, 34, 83), 30, color));
        return b;
    }

    private TextView lightBadge(String s, int color) {
        int brightness = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
        TextView b = text(s, 10, brightness > 145 ? NAVY : Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), dp(3), dp(8), dp(3));
        b.setBackground(round(color, 50, color));
        return b;
    }

    private ImageView iconView(int resId, int fg, int bg, int size) {
        ImageView icon = new ImageView(this);
        icon.setImageResource(resId);
        icon.setColorFilter(fg);
        int pad = Math.max(dp(7), size / 5);
        icon.setPadding(pad, pad, pad, pad);
        icon.setBackground(gradient(bg, Color.rgb(10, 22, 60), 14, Color.rgb(52, 66, 125)));
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return icon;
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

    private GradientDrawable gradient(int start, int end, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
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
