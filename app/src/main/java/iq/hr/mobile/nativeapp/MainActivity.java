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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
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

import androidx.core.content.FileProvider;

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

    private static final String APP_VERSION = "R2.11.0";
    private static final int APP_VERSION_CODE = 21;
    private static final String DATA_URL = "https://raw.githubusercontent.com/muayedhassan/employees/main/data/employees.json";
    private static final String UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/muayedhassan/HRNativeAndroidR2/main/latest.json";
    private static final String UPDATE_MANIFEST_URL_FALLBACK = "https://raw.githubusercontent.com/muayedhassan/HRNativeAndroidR2/master/latest.json";
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
    private boolean isCheckingUpdate = false;
    private boolean isDownloadingUpdate = false;
    private UpdateInfo latestUpdate;
    private boolean roleConfigured = false;
    private Typeface titleTypeface;
    private Typeface bodyTypeface;
    private Typeface numberTypeface;
    private String employeeDirectoryFilter = "الكل";
    private String profileTab = "وظيفة";
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
        root.addView(space(10));
        root.addView(nativeWorkspaceNav("الرئيسية"));
        root.addView(space(10));
        root.addView(commandStatusPanel());
        root.addView(space(10));
        root.addView(commandModuleGrid());
        root.addView(space(10));
        root.addView(dataQualityOverviewPanel());
        root.addView(space(10));
        root.addView(branchDistributionPanel());
        root.addView(space(10));
        root.addView(recentNotesPanel());
        root.addView(space(10));
        root.addView(webReplicaSubTabs());
        root.addView(space(10));
        root.addView(webReplicaModeTabs());
        root.addView(space(10));
        root.addView(webReplicaSearchWorkspace());
        root.addView(space(10));
        root.addView(homeActionBar());

        root.addView(space(10));
        TextView footer = text("R2.11.0 Native Web Parity Workspace: تنقل موحد، جودة مستقلة، وسجل موظفين أوسع.", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
    }

    private LinearLayout nativeWorkspaceNav(String active) {
        LinearLayout box = card(14);
        box.setPadding(dp(7), dp(7), dp(7), dp(7));
        box.setBackground(gradient(Color.rgb(12, 22, 58), Color.rgb(8, 16, 44), 14, BORDER));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = chipsBar();
        workspaceNavButton(tabs, "الرئيسية", active, v -> showHome());
        workspaceNavButton(tabs, "الموظفون", active, v -> {
            employeeDirectoryFilter = "الكل";
            showEmployeeDirectory();
        });
        workspaceNavButton(tabs, "الجودة", active, v -> showDataQualityCenter());
        workspaceNavButton(tabs, "الملاحظات", active, v -> showManagerNotes());
        if (currentRole.equals(ROLE_ADMIN)) {
            workspaceNavButton(tabs, "الإدارة", active, v -> showAdminDashboard());
        }
        workspaceNavButton(tabs, "النظام", active, v -> showStatus());
        workspaceNavButton(tabs, "التحديث", active, v -> showUpdateCenter());
        scroll.addView(tabs);
        box.addView(scroll);
        return box;
    }

    private void workspaceNavButton(LinearLayout parent, String label, String active, View.OnClickListener listener) {
        Button b = chipButton(label, label.equals(active));
        b.setOnClickListener(listener);
        parent.addView(b);
        parent.addView(spaceW(7));
    }

    private LinearLayout webReplicaHeader() {
        LinearLayout header = card(18);
        header.setPadding(dp(12), dp(12), dp(12), dp(12));
        header.setBackground(gradient(Color.rgb(8, 17, 48), Color.rgb(21, 32, 88), 18, Color.rgb(58, 72, 135)));

        LinearLayout row = horizontal();
        row.addView(iconView(R.drawable.ic_hr_people, GOLD, Color.rgb(31, 43, 104), dp(44)));
        row.addView(spaceW(9));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text("سجل الموظفين", 20, Color.WHITE, true));
        titleBox.addView(space(2));
        titleBox.addView(text("مديرية زراعة صلاح الدين · " + APP_VERSION, 10, Color.rgb(190, 205, 240), false));
        row.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(lightBadge(roleLabel(), currentRole.equals(ROLE_ADMIN) ? GOLD : PURPLE));
        header.addView(row);

        header.addView(space(8));
        TextView status = text("الواجهة الآن تبدأ من السجل والبحث مباشرة، بنفس منطق نسخة الويب وليس لوحة اختصارات فقط.", 10, Color.rgb(207, 226, 244), false);
        header.addView(status);
        return header;
    }

    private LinearLayout webReplicaModeTabs() {
        LinearLayout box = card(14);
        box.setPadding(dp(7), dp(7), dp(7), dp(7));
        box.setBackground(gradient(Color.rgb(12, 22, 58), Color.rgb(8, 16, 44), 14, BORDER));

        LinearLayout tabs = horizontal();
        Button perm = chipButton("الدائميون " + permCount, "دائم".equals(employeeDirectoryFilter));
        perm.setOnClickListener(v -> {
            employeeDirectoryFilter = "دائم";
            showHome();
        });
        tabs.addView(perm, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tabs.addView(spaceW(6));

        Button cont = chipButton("العقود " + contCount, "عقد".equals(employeeDirectoryFilter));
        cont.setOnClickListener(v -> {
            employeeDirectoryFilter = "عقد";
            showHome();
        });
        tabs.addView(cont, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tabs.addView(spaceW(6));

        Button all = chipButton("الكل " + employees.size(), "الكل".equals(employeeDirectoryFilter));
        all.setOnClickListener(v -> {
            employeeDirectoryFilter = "الكل";
            showHome();
        });
        tabs.addView(all, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(tabs);
        return box;
    }

    private LinearLayout webReplicaStatsRow() {
        LinearLayout row = horizontal();
        row.addView(compactPill("الموظفون", String.valueOf(employees.size()), GOLD));
        row.addView(spaceW(6));
        row.addView(compactPill("النطاق", String.valueOf(filteredEmployeeCount()), PRIMARY));
        row.addView(spaceW(6));
        row.addView(compactPill("ملاحظات", String.valueOf(notes.size()), PURPLE));
        return row;
    }

    private LinearLayout webReplicaSubTabs() {
        LinearLayout box = card(14);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        box.setBackground(gradient(Color.rgb(12, 22, 58), Color.rgb(8, 16, 44), 14, BORDER));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = chipsBar();

        Button list = chipButton("القائمة", true);
        list.setOnClickListener(v -> showHome());
        tabs.addView(list);
        tabs.addView(spaceW(6));

        Button notesButton = chipButton("ملاحظات المدير", false);
        notesButton.setOnClickListener(v -> showManagerNotes());
        tabs.addView(notesButton);
        tabs.addView(spaceW(6));

        Button admin = chipButton("الإدارة", false);
        admin.setOnClickListener(v -> showAdminDashboard());
        tabs.addView(admin);
        tabs.addView(spaceW(6));

        Button quality = chipButton("الجودة", false);
        quality.setOnClickListener(v -> showDataQualityCenter());
        tabs.addView(quality);
        tabs.addView(spaceW(6));

        Button updates = chipButton("التحديثات", false);
        updates.setOnClickListener(v -> showUpdateCenter());
        tabs.addView(updates);
        tabs.addView(spaceW(6));

        Button system = chipButton("النظام", false);
        system.setOnClickListener(v -> showStatus());
        tabs.addView(system);

        scroll.addView(tabs);
        box.addView(scroll);
        return box;
    }

    private LinearLayout webReplicaSearchWorkspace() {
        LinearLayout box = card(16);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 16, BORDER));

        LinearLayout title = horizontal();
        title.addView(iconView(R.drawable.ic_hr_search, GOLD, Color.rgb(22, 34, 83), dp(36)));
        title.addView(spaceW(8));
        LinearLayout titleText = new LinearLayout(this);
        titleText.setOrientation(LinearLayout.VERTICAL);
        titleText.addView(text("البحث الإداري السريع", 15, TEXT, true));
        titleText.addView(text("القائمة تظهر مباشرة، والبحث يصفّي النتائج مثل نسخة الويب.", 9, MUTED, false));
        title.addView(titleText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(title);
        box.addView(space(8));

        EditText search = editText("ابحث بالاسم، الرقم الوظيفي، الشعبة، أو اسم الأم...");
        search.setSingleLine(true);
        box.addView(search);
        box.addView(space(7));
        TextView status = text("يعرض أول الموظفين ضمن النطاق الحالي", 10, MUTED, false);
        box.addView(status);
        box.addView(space(8));

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        box.addView(results);
        renderHomeEmployeeResults("", results, status);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderHomeEmployeeResults(s.toString(), results, status);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        return box;
    }

    private void renderHomeEmployeeResults(String query, LinearLayout container, TextView status) {
        container.removeAllViews();
        String q = normalize(query);
        int shown = 0;
        int matched = 0;
        for (Employee e : employees) {
            boolean match = q.length() < 2 || employeeMatches(e, q);
            if (employeePassesDirectoryFilter(e) && match) {
                matched++;
                if (shown < 12) {
                    container.addView(employeeCard(e));
                    container.addView(space(6));
                    shown++;
                }
            }
        }
        if (matched == 0) {
            container.addView(emptyCard("لا توجد نتائج مطابقة ضمن النطاق الحالي"));
        }
        if (q.length() < 2) {
            status.setText("النطاق: " + employeeDirectoryFilter + " · عرض أول " + shown + " من " + filteredEmployeeCount());
        } else {
            status.setText("نتائج البحث: " + matched + (matched > 12 ? " · عُرضت أول 12 نتيجة" : ""));
        }
    }

    private LinearLayout dataQualityOverviewPanel() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, Color.rgb(42, 56, 118)));

        LinearLayout top = horizontal();
        top.addView(iconView(R.drawable.ic_hr_quality, ORANGE, Color.rgb(22, 34, 83), dp(40)));
        top.addView(spaceW(9));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("مركز جودة البيانات", 16, TEXT, true));
        labels.addView(text("قراءة مباشرة لاكتمال ملفات الموظفين", 10, MUTED, false));
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(top);

        int average = averageProfileCompletion();
        int incomplete = incompleteProfileCount();
        box.addView(space(10));
        LinearLayout metrics = horizontal();
        metrics.addView(compactPill("متوسط الاكتمال", average + "%", average >= 75 ? GREEN : ORANGE));
        metrics.addView(spaceW(8));
        metrics.addView(compactPill("ملفات تحتاج مراجعة", String.valueOf(incomplete), incomplete == 0 ? GREEN : RED));
        metrics.addView(spaceW(8));
        metrics.addView(compactPill("مصدر البيانات", dataVersion.length() > 12 ? "GitHub" : dataVersion, PRIMARY));
        box.addView(metrics);

        box.addView(space(10));
        box.addView(progressStrip("اكتمال السجل العام", average, average >= 75 ? GREEN : ORANGE));
        box.addView(space(8));
        TextView hint = text("اضغط على أي بطاقة موظف لعرض النواقص داخل تبويب جودة.", 10, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        box.addView(hint);
        return box;
    }

    private LinearLayout branchDistributionPanel() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(gradient(Color.rgb(12, 22, 58), Color.rgb(8, 16, 44), 18, BORDER));

        LinearLayout top = horizontal();
        top.addView(iconView(R.drawable.ic_hr_people, GOLD, Color.rgb(22, 34, 83), dp(40)));
        top.addView(spaceW(9));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text("توزيع الشعب", 16, TEXT, true));
        titleBox.addView(text("أكثر الشعب ظهورًا في بيانات الموظفين", 10, MUTED, false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(top);
        box.addView(space(10));

        List<String> labels = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Employee e : employees) {
            String branch = safe(e.branch);
            if ("-".equals(branch)) branch = "غير محدد";
            int index = labels.indexOf(branch);
            if (index >= 0) {
                counts.set(index, counts.get(index) + 1);
            } else {
                labels.add(branch);
                counts.add(1);
            }
        }

        if (labels.isEmpty()) {
            box.addView(emptyCard("لا توجد شعب متاحة حاليًا"));
            return box;
        }

        for (int i = 0; i < Math.min(5, labels.size()); i++) {
            int topIndex = topBranchIndex(counts);
            String label = labels.get(topIndex);
            int count = counts.get(topIndex);
            int percent = employees.isEmpty() ? 0 : Math.round((count * 100f) / employees.size());
            box.addView(branchDistributionRow(label, count, percent, branchAccent(i)));
            counts.set(topIndex, -1);
            if (i < Math.min(5, labels.size()) - 1) box.addView(space(7));
        }
        return box;
    }

    private LinearLayout recentNotesPanel() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, BORDER));

        LinearLayout top = horizontal();
        top.addView(iconView(R.drawable.ic_hr_notes, PURPLE, Color.rgb(22, 34, 83), dp(40)));
        top.addView(spaceW(9));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("آخر ملاحظات المدير", 16, TEXT, true));
        labels.addView(text("متابعة مختصرة قبل فتح السجل الكامل", 10, MUTED, false));
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(top);
        box.addView(space(10));

        if (notes.isEmpty()) {
            box.addView(emptyCard("لا توجد ملاحظات محفوظة بعد"));
        } else {
            for (int i = 0; i < Math.min(3, notes.size()); i++) {
                box.addView(compactNoteRow(notes.get(i)));
                if (i < Math.min(3, notes.size()) - 1) box.addView(space(7));
            }
        }

        box.addView(space(10));
        Button open = outlineButton("فتح سجل الملاحظات");
        open.setOnClickListener(v -> showManagerNotes());
        box.addView(open);
        return box;
    }

    private LinearLayout progressStrip(String label, int percent, int accent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(9), dp(8), dp(9), dp(8));
        box.setBackground(round(Color.rgb(16, 27, 78), 13, Color.rgb(38, 52, 100)));

        LinearLayout row = horizontal();
        row.addView(text(label, 11, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView value = text(percent + "%", 13, accent, true);
        value.setGravity(Gravity.CENTER);
        value.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        row.addView(value);
        box.addView(row);
        box.addView(space(7));

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(round(Color.rgb(8, 15, 42), 10, Color.rgb(31, 43, 85)));
        View fill = new View(this);
        fill.setBackground(round(accent, 10, accent));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(8), Math.max(1, percent)));
        View rest = new View(this);
        track.addView(rest, new LinearLayout.LayoutParams(0, dp(8), Math.max(1, 100 - percent)));
        box.addView(track);
        return box;
    }

    private LinearLayout branchDistributionRow(String branch, int count, int percent, int accent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(9), dp(8), dp(9), dp(8));
        row.setBackground(round(Color.rgb(16, 27, 78), 13, Color.rgb(38, 52, 100)));

        LinearLayout labels = horizontal();
        TextView title = text(branch, 12, TEXT, true);
        labels.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView value = text(count + " · " + percent + "%", 12, accent, true);
        value.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        labels.addView(value);
        row.addView(labels);
        row.addView(space(6));
        row.addView(progressStrip("حصة الشعبة", percent, accent));
        return row;
    }

    private LinearLayout compactNoteRow(ManagerNote note) {
        LinearLayout row = card(13);
        row.setPadding(dp(10), dp(9), dp(10), dp(9));
        row.setBackground(round(Color.rgb(16, 27, 78), 13, Color.rgb(38, 52, 100)));
        LinearLayout top = horizontal();
        top.addView(miniBadge(note.type, movementColor(note.type)));
        top.addView(spaceW(6));
        top.addView(miniBadge("new".equals(note.status) ? "جديد" : "مراجع", "new".equals(note.status) ? ORANGE : GREEN));
        row.addView(top);
        row.addView(space(6));
        row.addView(text(note.employee, 13, TEXT, true));
        row.addView(text(safe(note.fromBranch) + (note.toBranch.length() > 0 ? " ← " + note.toBranch : ""), 10, MUTED, false));
        return row;
    }

    private int averageProfileCompletion() {
        if (employees.isEmpty()) return 0;
        int sum = 0;
        for (Employee e : employees) sum += profileCompletion(e);
        return Math.round(sum / (float) employees.size());
    }

    private int incompleteProfileCount() {
        int count = 0;
        for (Employee e : employees) {
            if (profileMissingCount(e) > 0) count++;
        }
        return count;
    }

    private int topBranchIndex(List<Integer> counts) {
        int best = 0;
        for (int i = 1; i < counts.size(); i++) {
            if (counts.get(i) > counts.get(best)) best = i;
        }
        return best;
    }

    private int branchAccent(int index) {
        if (index == 0) return GOLD;
        if (index == 1) return GREEN;
        if (index == 2) return PRIMARY;
        if (index == 3) return PURPLE;
        return ORANGE;
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
            showDataQualityCenter();
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

    private void showDataQualityCenter() {
        baseScreen();
        LinearLayout header = screenHero("مركز جودة البيانات", "تحليل النواقص واكتمال ملفات الموظفين", R.drawable.ic_hr_quality, ORANGE);
        header.addView(space(12));
        LinearLayout badges = horizontal();
        badges.addView(badge("اكتمال " + averageProfileCompletion() + "%", averageProfileCompletion() >= 75 ? GREEN : ORANGE));
        badges.addView(spaceW(8));
        badges.addView(badge("نواقص " + incompleteProfileCount(), incompleteProfileCount() == 0 ? GREEN : RED));
        badges.addView(spaceW(8));
        badges.addView(badge(APP_VERSION, PRIMARY));
        header.addView(badges);
        root.addView(header);

        root.addView(space(10));
        root.addView(nativeWorkspaceNav("الجودة"));
        root.addView(space(10));
        root.addView(dataQualityOverviewPanel());
        root.addView(space(10));
        root.addView(missingFieldsPanel());
        root.addView(space(10));
        root.addView(incompleteEmployeesPanel());

        root.addView(space(12));
        Button list = primaryButton("فتح سجل الموظفين مع فلتر النواقص");
        list.setOnClickListener(v -> {
            employeeDirectoryFilter = "نواقص";
            showEmployeeDirectory();
        });
        root.addView(list);
        root.addView(space(8));
        Button back = outlineButton("الرجوع إلى الرئيسية");
        back.setOnClickListener(v -> showHome());
        root.addView(back);
    }

    private LinearLayout missingFieldsPanel() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, BORDER));
        box.addView(text("أكثر الحقول الناقصة", 16, TEXT, true));
        box.addView(space(8));

        String[] fields = {"الرقم الوظيفي", "الشعبة", "العنوان الوظيفي", "الدرجة", "المرحلة", "التحصيل", "اسم الأم", "رقم الهوية", "تاريخ التعيين"};
        int[] counts = new int[fields.length];
        for (Employee e : employees) {
            List<String> missing = missingFieldLabels(e);
            for (int i = 0; i < fields.length; i++) {
                if (missing.contains(fields[i])) counts[i]++;
            }
        }

        for (int i = 0; i < Math.min(5, fields.length); i++) {
            int index = topMissingIndex(counts);
            if (counts[index] <= 0) {
                if (i == 0) box.addView(emptyCard("لا توجد نواقص أساسية ظاهرة حاليًا"));
                break;
            }
            int percent = employees.isEmpty() ? 0 : Math.round((counts[index] * 100f) / employees.size());
            box.addView(progressStrip(fields[index] + " · " + counts[index] + " ملف", percent, branchAccent(i)));
            counts[index] = -1;
            if (i < 4) box.addView(space(7));
        }
        return box;
    }

    private LinearLayout incompleteEmployeesPanel() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(gradient(Color.rgb(12, 22, 58), Color.rgb(8, 16, 44), 18, BORDER));
        LinearLayout top = horizontal();
        top.addView(iconView(R.drawable.ic_hr_search, GOLD, Color.rgb(22, 34, 83), dp(38)));
        top.addView(spaceW(8));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("ملفات تحتاج مراجعة", 16, TEXT, true));
        labels.addView(text("أول الملفات ذات النواقص الأساسية", 10, MUTED, false));
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(top);
        box.addView(space(10));

        int shown = 0;
        for (Employee e : employees) {
            if (profileMissingCount(e) > 0) {
                box.addView(qualityEmployeeRow(e));
                box.addView(space(7));
                shown++;
                if (shown == 8) break;
            }
        }
        if (shown == 0) box.addView(emptyCard("لا توجد ملفات ناقصة ضمن الحقول الأساسية"));
        return box;
    }

    private LinearLayout qualityEmployeeRow(Employee e) {
        LinearLayout row = card(13);
        row.setPadding(dp(10), dp(9), dp(10), dp(9));
        row.setBackground(round(Color.rgb(16, 27, 78), 13, Color.rgb(38, 52, 100)));
        row.setOnClickListener(v -> {
            profileTab = "جودة";
            showEmployeeProfile(e);
        });
        LinearLayout top = horizontal();
        TextView avatar = text(cardInitials(e.name), 12, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(profileCompletion(e) >= 75 ? GREEN : ORANGE, 12, profileCompletion(e) >= 75 ? GREEN : ORANGE));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(36), dp(36)));
        top.addView(spaceW(8));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(e.name, 13, TEXT, true));
        labels.addView(text(safe(e.branch) + " · نواقص: " + profileMissingCount(e), 10, MUTED, false));
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(miniBadge(profileCompletion(e) + "%", profileCompletion(e) >= 75 ? GREEN : ORANGE));
        row.addView(top);
        return row;
    }

    private int topMissingIndex(int[] counts) {
        int best = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[best]) best = i;
        }
        return best;
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

        root.addView(space(10));
        root.addView(nativeWorkspaceNav("الموظفون"));
        root.addView(space(10));
        root.addView(directoryWorkspaceSummary());

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
        String[] employeeFilters = {"الكل", "دائم", "عقد", "نواقص"};
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

    private LinearLayout directoryWorkspaceSummary() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, BORDER));
        box.addView(text("ملخص مساحة الموظفين", 16, TEXT, true));
        box.addView(space(8));
        LinearLayout row = horizontal();
        row.addView(compactPill("النطاق الحالي", employeeDirectoryFilter, PRIMARY));
        row.addView(spaceW(8));
        row.addView(compactPill("عدد النتائج", String.valueOf(filteredEmployeeCount()), GOLD));
        row.addView(spaceW(8));
        row.addView(compactPill("ملفات ناقصة", String.valueOf(incompleteProfileCount()), incompleteProfileCount() == 0 ? GREEN : RED));
        box.addView(row);
        box.addView(space(10));
        Button quality = outlineButton("فتح مركز جودة البيانات");
        quality.setOnClickListener(v -> showDataQualityCenter());
        box.addView(quality);
        return box;
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
        if ("نواقص".equals(employeeDirectoryFilter)) return profileMissingCount(e) > 0;
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

        if (profileTab == null || profileTab.length() == 0) profileTab = "وظيفة";
        root.addView(employeeDigitalIdCard(e));
        root.addView(space(10));
        root.addView(profileTabsBar(e));
        root.addView(space(10));
        root.addView(profileTabContent(e));
        root.addView(space(12));
        root.addView(profileActionDock(e));
    }

    private LinearLayout employeeDigitalIdCard(Employee e) {
        LinearLayout card = card(20);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(gradient(Color.rgb(9, 20, 67), Color.rgb(5, 10, 32), 20, Color.rgb(82, 93, 151)));

        LinearLayout top = horizontal();
        ImageView mark = iconView(R.drawable.ic_hr_profile, NAVY, GOLD, dp(44));
        top.addView(mark);
        top.addView(spaceW(9));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.RIGHT);
        titleBox.addView(text("مديرية زراعة صلاح الدين", 13, Color.WHITE, true));
        titleBox.addView(text("بطاقة تعريف وظيفية Native", 10, Color.rgb(190, 205, 240), false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(miniBadge(APP_VERSION, PRIMARY));
        card.addView(top);

        card.addView(space(12));
        LinearLayout identity = horizontal();
        TextView avatar = text(cardInitials(e.name), 20, Color.rgb(13, 24, 68), true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(gradient(GOLD, Color.rgb(255, 177, 55), 22, GOLD));
        identity.addView(avatar, new LinearLayout.LayoutParams(dp(66), dp(66)));
        identity.addView(spaceW(10));

        LinearLayout nameBox = new LinearLayout(this);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        nameBox.setGravity(Gravity.RIGHT);
        nameBox.addView(text(e.name, 20, Color.WHITE, true));
        nameBox.addView(space(2));
        nameBox.addView(text(safe(e.jobTitle), 12, Color.rgb(220, 232, 255), false));
        nameBox.addView(space(3));
        LinearLayout badges = horizontal();
        badges.addView(lightBadge(e.typeLabel(), "عقد".equals(e.typeLabel()) ? ORANGE : GREEN));
        badges.addView(spaceW(6));
        badges.addView(lightBadge(safe(e.branch), Color.rgb(27, 43, 102)));
        nameBox.addView(badges);
        identity.addView(nameBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(identity);

        card.addView(space(12));
        LinearLayout row1 = horizontal();
        row1.addView(profileIdField("الرقم", safe(e.id), PRIMARY));
        row1.addView(spaceW(7));
        row1.addView(profileIdField("اكتمال", profileCompletion(e) + "%", profileCompletion(e) >= 75 ? GREEN : ORANGE));
        card.addView(row1);
        card.addView(space(7));
        LinearLayout row2 = horizontal();
        row2.addView(profileIdField("الدرجة", safe(e.grade), PURPLE));
        row2.addView(spaceW(7));
        row2.addView(profileIdField("المرحلة", safe(e.step), GOLD));
        card.addView(row2);

        card.addView(space(12));
        LinearLayout footer = horizontal();
        footer.addView(text("HR-" + safe(e.id) + " · " + e.typeLabel(), 11, GOLD, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        footer.addView(text("GitHub Data", 10, Color.rgb(190, 205, 240), false));
        card.addView(footer);
        return card;
    }

    private LinearLayout profileIdField(String label, String value, int accent) {
        LinearLayout box = card(12);
        box.setPadding(dp(9), dp(7), dp(9), dp(7));
        box.setBackground(gradient(Color.rgb(16, 28, 78), Color.rgb(9, 18, 50), 12, Color.rgb(45, 58, 112)));
        box.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView labelView = text(label, 9, MUTED, false);
        TextView valueView = text(value, 15, accent, true);
        if (hasDigit(value)) valueView.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        box.addView(labelView);
        box.addView(valueView);
        return box;
    }

    private HorizontalScrollView profileTabsBar(Employee e) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout bar = chipsBar();
        bar.setPadding(0, dp(2), 0, dp(2));
        String[] tabs = {"وظيفة", "هوية", "تعليم", "جودة", "ملاحظات"};
        for (String item : tabs) {
            final String tab = item;
            Button b = chipButton(tab, tab.equals(profileTab));
            b.setOnClickListener(v -> {
                profileTab = tab;
                showEmployeeProfile(e);
            });
            bar.addView(b, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)));
            bar.addView(spaceW(7));
        }
        scroll.addView(bar);
        return scroll;
    }

    private LinearLayout profileTabContent(Employee e) {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(gradient(Color.rgb(11, 21, 58), Color.rgb(8, 15, 42), 18, BORDER));
        box.addView(profileTabHeader(e));
        box.addView(space(10));

        if ("هوية".equals(profileTab)) {
            addFieldGridRow(box,
                    profileFieldCard("الاسم الكامل", safe(e.name), PRIMARY),
                    profileFieldCard("اسم الأم", safe(e.motherName), PURPLE));
            addFieldGridRow(box,
                    profileFieldCard("الجنس", safe(e.gender), GREEN),
                    profileFieldCard("تاريخ الولادة", shortDate(e.birthDate), GOLD));
            addFieldGridRow(box,
                    profileFieldCard("رقم الهوية", safe(e.identityNo), ORANGE),
                    profileFieldCard("جهة الإصدار", safe(e.identityIssuer), PRIMARY));
            addFieldGridRow(box,
                    profileFieldCard("تاريخ الإصدار", shortDate(e.identityIssueDate), PURPLE),
                    profileFieldCard("حالة الملف", profileMissingCount(e) == 0 ? "مكتمل" : "يحتاج مراجعة", profileMissingCount(e) == 0 ? GREEN : RED));
        } else if ("تعليم".equals(profileTab)) {
            addFieldGridRow(box,
                    profileFieldCard("التحصيل الدراسي", safe(e.education), GREEN),
                    profileFieldCard("الاختصاص", safe(e.specialization), PRIMARY));
            addFieldGridRow(box,
                    profileFieldCard("آخر تحديث", shortDate(e.sourceUpdatedAt), GOLD),
                    profileFieldCard("مصدر البيانات", "GitHub Employees JSON", PURPLE));
            box.addView(space(4));
            box.addView(infoStrip("هذه الصفحة تضع التعليم والاختصاص وتاريخ التحديث في منطقة واحدة مثل بطاقة الويب بدل توزيعها داخل نص طويل."));
        } else if ("جودة".equals(profileTab)) {
            LinearLayout metrics1 = horizontal();
            metrics1.addView(profileMetric("اكتمال الملف", profileCompletion(e) + "%", profileCompletion(e) >= 75 ? GREEN : ORANGE));
            metrics1.addView(spaceW(8));
            metrics1.addView(profileMetric("النواقص", String.valueOf(profileMissingCount(e)), profileMissingCount(e) == 0 ? GREEN : RED));
            box.addView(metrics1);
            box.addView(space(8));
            box.addView(dataQualityCard(e));
        } else if ("ملاحظات".equals(profileTab)) {
            addFieldGridRow(box,
                    profileFieldCard("وضع الجهاز", roleLabel(), PRIMARY),
                    profileFieldCard("الشعبة الحالية", safe(e.branch), GOLD));
            box.addView(space(6));
            box.addView(infoStrip("يمكن اختيار هذا الموظف مباشرة لملاحظات النقل أو التنسيب. الحفظ الحالي محلي داخل الجهاز إلى أن يتم ربط Google Sheet Sync."));
            box.addView(space(10));
            Button note = primaryButton("فتح ملاحظات المدير لهذا الموظف");
            note.setOnClickListener(v -> {
                selectedEmployee = e;
                showManagerNotes();
            });
            box.addView(note);
        } else {
            addFieldGridRow(box,
                    profileFieldCard("الشعبة", safe(e.branch), PRIMARY),
                    profileFieldCard("العنوان الوظيفي", safe(e.jobTitle), GREEN));
            addFieldGridRow(box,
                    profileFieldCard("الحالة", safe(e.type), "عقد".equals(e.typeLabel()) ? ORANGE : GREEN),
                    profileFieldCard("نوع التوظيف", e.typeLabel(), "عقد".equals(e.typeLabel()) ? ORANGE : GREEN));
            addFieldGridRow(box,
                    profileFieldCard("الدرجة", safe(e.grade), PURPLE),
                    profileFieldCard("المرحلة", safe(e.step), GOLD));
            addFieldGridRow(box,
                    profileFieldCard("الراتب", formatSalary(e.salary), PRIMARY),
                    profileFieldCard("تاريخ التعيين", shortDate(e.hireDate), GREEN));
        }
        return box;
    }

    private LinearLayout profileTabHeader(Employee e) {
        LinearLayout header = horizontal();
        ImageView icon = iconView(profileTabIcon(), Color.WHITE, profileTabColor(), dp(38));
        header.addView(icon);
        header.addView(spaceW(8));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.RIGHT);
        titles.addView(text("تبويب " + profileTab, 16, TEXT, true));
        titles.addView(text(profileTabSubtitle(e), 10, MUTED, false));
        header.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return header;
    }

    private int profileTabIcon() {
        if ("هوية".equals(profileTab)) return R.drawable.ic_hr_people;
        if ("تعليم".equals(profileTab)) return R.drawable.ic_hr_quality;
        if ("جودة".equals(profileTab)) return R.drawable.ic_hr_sync;
        if ("ملاحظات".equals(profileTab)) return R.drawable.ic_hr_notes;
        return R.drawable.ic_hr_profile;
    }

    private int profileTabColor() {
        if ("هوية".equals(profileTab)) return PURPLE;
        if ("تعليم".equals(profileTab)) return GREEN;
        if ("جودة".equals(profileTab)) return ORANGE;
        if ("ملاحظات".equals(profileTab)) return PRIMARY;
        return GOLD;
    }

    private String profileTabSubtitle(Employee e) {
        if ("هوية".equals(profileTab)) return "بيانات شخصية وهوية مدنية مركزة";
        if ("تعليم".equals(profileTab)) return "التحصيل والاختصاص وآخر تحديث";
        if ("جودة".equals(profileTab)) return "اكتمال الحقول والنواقص المطلوبة";
        if ("ملاحظات".equals(profileTab)) return "اختيار الموظف وإرسال الملاحظة";
        return safe(e.branch) + " · " + safe(e.jobTitle);
    }

    private LinearLayout profileFieldCard(String label, String value, int accent) {
        LinearLayout c = card(13);
        c.setPadding(dp(10), dp(9), dp(10), dp(9));
        c.setBackground(gradient(Color.rgb(14, 25, 70), Color.rgb(8, 17, 47), 13, Color.rgb(42, 54, 105)));
        c.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView labelView = text(label, 9, MUTED, false);
        TextView valueView = text(value, 13, TEXT, true);
        if (hasDigit(value)) valueView.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        View accentLine = new View(this);
        accentLine.setBackgroundColor(accent);
        c.addView(accentLine, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        c.addView(space(6));
        c.addView(labelView);
        c.addView(space(2));
        c.addView(valueView);
        return c;
    }

    private void addFieldGridRow(LinearLayout parent, LinearLayout first, LinearLayout second) {
        LinearLayout row = horizontal();
        row.addView(first);
        row.addView(spaceW(8));
        row.addView(second);
        parent.addView(row);
        parent.addView(space(8));
    }

    private TextView infoStrip(String message) {
        TextView strip = text(message, 11, Color.rgb(218, 226, 255), false);
        strip.setPadding(dp(10), dp(8), dp(10), dp(8));
        strip.setBackground(round(Color.rgb(15, 26, 74), 14, Color.rgb(42, 54, 105)));
        return strip;
    }

    private LinearLayout profileActionDock(Employee e) {
        LinearLayout dock = card(18);
        dock.setPadding(dp(11), dp(11), dp(11), dp(11));
        dock.setBackground(gradient(Color.rgb(12, 23, 66), Color.rgb(8, 15, 42), 18, BORDER));
        dock.addView(text("إجراءات الملف", 14, TEXT, true));
        dock.addView(space(8));
        LinearLayout row1 = horizontal();
        Button notesButton = primaryButton(currentRole.equals(ROLE_HR) ? "اختيار للملاحظات" : "فتح المراجعة");
        notesButton.setOnClickListener(v -> {
            selectedEmployee = e;
            showManagerNotes();
        });
        row1.addView(notesButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row1.addView(spaceW(7));
        Button listButton = outlineButton("القائمة");
        listButton.setOnClickListener(v -> showEmployeeDirectory());
        row1.addView(listButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        dock.addView(row1);
        dock.addView(space(8));
        Button home = outlineButton("الرجوع إلى الرئيسية");
        home.setOnClickListener(v -> showHome());
        dock.addView(home);
        return dock;
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
        root.addView(space(10));
        root.addView(nativeWorkspaceNav("النظام"));
        root.addView(space(12));
        LinearLayout box = card(18);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, BORDER));
        box.addView(text("الإصدار: " + APP_VERSION, 14, TEXT, true));
        box.addView(space(6));
        box.addView(text("رابط البيانات:\n" + DATA_URL, 12, MUTED, false));
        box.addView(space(6));
        box.addView(text("عدد الموظفين المحلي: " + employees.size(), 13, TEXT, false));
        box.addView(text("دائم: " + permCount + " — عقود: " + contCount, 13, TEXT, false));
        box.addView(text("نسخة البيانات: " + dataVersion, 13, MUTED, false));
        box.addView(text("آخر تحديث: " + lastSync, 13, MUTED, false));
        box.addView(space(10));
        LinearLayout row = horizontal();
        row.addView(compactPill("نوع الجهاز", roleLabel(), currentRole.equals(ROLE_ADMIN) ? GOLD : PURPLE));
        row.addView(spaceW(8));
        row.addView(compactPill("جودة", averageProfileCompletion() + "%", averageProfileCompletion() >= 75 ? GREEN : ORANGE));
        row.addView(spaceW(8));
        row.addView(compactPill("ملاحظات", String.valueOf(notes.size()), PRIMARY));
        box.addView(row);
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

        root.addView(space(10));
        root.addView(nativeWorkspaceNav("الإدارة"));
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

        root.addView(space(10));
        root.addView(nativeWorkspaceNav("التحديث"));
        root.addView(space(12));
        root.addView(smartUpdateCard());

        root.addView(space(12));
        LinearLayout box = card(18);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.addView(text("آلية التحديث اليدوية الاحتياطية", 18, TEXT, true));
        box.addView(space(8));
        box.addView(text("1. فك ملف Patch zip فوق مشروع HRNativeAndroidR2\n2. نفذ git add -A ثم commit ثم push\n3. افتح GitHub Actions وانتظر نجاح البناء\n4. حمّل Artifact الناتج\n5. افتح app-release.apk وثبّته فوق النسخة السابقة", 13, TEXT, false));
        box.addView(space(12));
        box.addView(text("مهم: استخدم فقط APK الناتج من GitHub Actions لأن توقيعه ثابت، أما APK المحلي أو debug فقد لا يثبت فوق النسخة السابقة.", 12, RED, true));
        root.addView(box);

        root.addView(space(12));
        LinearLayout release = card(18);
        release.setPadding(dp(16), dp(14), dp(16), dp(14));
        release.addView(text("محتوى R2.11.0", 18, TEXT, true));
        release.addView(space(8));
        release.addView(text("• Workspace تنقل موحد بين الشاشات الأساسية\n• مركز جودة بيانات مستقل\n• فلتر نواقص داخل سجل الموظفين\n• ملخص أوسع لسجل الموظفين\n• حالة نظام تعرض النوع والجودة والملاحظات", 13, TEXT, false));
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

    private LinearLayout smartUpdateCard() {
        LinearLayout box = card(18);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, BORDER));
        box.addView(text("التحديث الذكي داخل التطبيق", 18, TEXT, true));
        box.addView(space(6));
        box.addView(text("يفحص ملف latest.json من GitHub. عند توفر إصدار جديد، يحمّل APK ويفتح شاشة التثبيت مباشرة.", 12, MUTED, false));
        box.addView(space(10));

        String state;
        int stateColor = MUTED;
        if (isCheckingUpdate) {
            state = "جاري فحص آخر إصدار...";
            stateColor = ORANGE;
        } else if (isDownloadingUpdate) {
            state = "جاري تحميل ملف APK...";
            stateColor = ORANGE;
        } else if (latestUpdate == null) {
            state = "لم يتم الفحص بعد";
        } else if (latestUpdate.versionCode > APP_VERSION_CODE) {
            state = "تحديث متاح: " + latestUpdate.versionName + " / code " + latestUpdate.versionCode;
            stateColor = GREEN;
        } else {
            state = "أنت على آخر إصدار متاح: " + APP_VERSION;
            stateColor = GREEN;
        }
        box.addView(text(state, 13, stateColor, true));

        if (latestUpdate != null && latestUpdate.notes.length() > 0) {
            box.addView(space(8));
            box.addView(text(latestUpdate.notes, 12, TEXT, false));
        }

        box.addView(space(12));
        LinearLayout actions = horizontal();
        Button check = primaryButton(isCheckingUpdate ? "جاري الفحص..." : "فحص التحديث");
        check.setEnabled(!isCheckingUpdate && !isDownloadingUpdate);
        check.setOnClickListener(v -> checkForAppUpdate());
        actions.addView(check, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(spaceW(8));

        Button install = outlineButton(isDownloadingUpdate ? "جاري التحميل..." : "تحميل وتثبيت");
        boolean canInstall = latestUpdate != null && latestUpdate.versionCode > APP_VERSION_CODE && latestUpdate.apkUrl.length() > 0;
        install.setEnabled(canInstall && !isCheckingUpdate && !isDownloadingUpdate);
        install.setOnClickListener(v -> downloadAndInstallUpdate(latestUpdate));
        actions.addView(install, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(actions);

        box.addView(space(10));
        box.addView(text("ملاحظة: Android سيطلب موافقة المستخدم على التثبيت. التثبيت الصامت يحتاج Google Play أو MDM.", 11, RED, true));
        return box;
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

        root.addView(space(10));
        root.addView(nativeWorkspaceNav("الملاحظات"));
        root.addView(space(12));
        root.addView(statsPanel());
        root.addView(space(12));
        root.addView(managerWorkflowBoard());
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

    private LinearLayout managerWorkflowBoard() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, Color.rgb(42, 56, 118)));

        LinearLayout top = horizontal();
        top.addView(iconView(R.drawable.ic_hr_notes, PURPLE, Color.rgb(22, 34, 83), dp(40)));
        top.addView(spaceW(9));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("لوحة سير الملاحظات", 16, TEXT, true));
        labels.addView(text("اختيار، حركة، إرسال، مراجعة", 10, MUTED, false));
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(top);
        box.addView(space(10));

        LinearLayout steps = horizontal();
        steps.addView(workflowStep("1", "الموظف", selectedEmployee == null ? "لم يحدد" : selectedEmployee.name, selectedEmployee == null ? ORANGE : GREEN));
        steps.addView(spaceW(7));
        steps.addView(workflowStep("2", "الحركة", currentMovement, movementColor(currentMovement)));
        steps.addView(spaceW(7));
        steps.addView(workflowStep("3", "السجل", countNewNotes() + " جديد", countNewNotes() == 0 ? GREEN : RED));
        box.addView(steps);
        box.addView(space(10));
        box.addView(progressStrip("جاهزية سير العمل", selectedEmployee == null && currentRole.equals(ROLE_HR) ? 55 : 90, selectedEmployee == null && currentRole.equals(ROLE_HR) ? ORANGE : GREEN));
        return box;
    }

    private LinearLayout workflowStep(String number, String title, String value, int accent) {
        LinearLayout step = card(13);
        step.setPadding(dp(8), dp(7), dp(8), dp(7));
        step.setBackground(round(Color.rgb(16, 27, 78), 13, Color.rgb(38, 52, 100)));
        step.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView n = text(number, 16, accent, true);
        n.setGravity(Gravity.CENTER);
        n.setTypeface(numberTypeface == null ? Typeface.MONOSPACE : numberTypeface, Typeface.BOLD);
        TextView t = text(title, 9, MUTED, false);
        t.setGravity(Gravity.CENTER);
        TextView v = text(value, 10, TEXT, true);
        v.setGravity(Gravity.CENTER);
        step.addView(n);
        step.addView(t);
        step.addView(v);
        return step;
    }

    private LinearLayout managerForm() {
        LinearLayout form = card(18);
        form.setPadding(dp(16), dp(16), dp(16), dp(16));
        form.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, BORDER));
        form.addView(text("محطة إرسال الملاحظة", 18, TEXT, true));
        form.addView(space(8));
        form.addView(text("ابحث عن الموظف، اختر نوع الحركة، ثم أرسل الملاحظة. يتم حفظ كل شيء محليًا حالياً.", 12, MUTED, false));
        form.addView(space(12));

        EditText search = editText("اكتب اسم الموظف للبحث...");
        search.setSingleLine(true);
        form.addView(search);
        form.addView(space(8));

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        form.addView(results);

        TextView selected = text(selectedEmployee == null ? "لم يتم اختيار موظف" : "الموظف المختار: " + selectedEmployee.name + "\nالشعبة الحالية: " + selectedEmployee.branch + "\nنوع التوظيف: " + selectedEmployee.typeLabel(), 13, selectedEmployee == null ? MUTED : TEXT, false);
        selected.setPadding(dp(10), dp(9), dp(10), dp(9));
        selected.setBackground(round(Color.rgb(16, 27, 78), 13, selectedEmployee == null ? BORDER : GREEN));
        form.addView(selected);
        form.addView(space(10));
        form.addView(movementCommandDeck());
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
                                selected.setText("الموظف المختار: " + e.name + "\nالشعبة الحالية: " + e.branch + "\nنوع التوظيف: " + e.typeLabel());
                                selected.setTextColor(TEXT);
                                selected.setBackground(round(Color.rgb(16, 27, 78), 13, GREEN));
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

    private LinearLayout movementCommandDeck() {
        LinearLayout deck = new LinearLayout(this);
        deck.setOrientation(LinearLayout.VERTICAL);
        deck.addView(text("نوع الحركة", 14, TEXT, true));
        deck.addView(space(7));

        LinearLayout r1 = horizontal();
        r1.addView(movementCard("ملاحظة", "توجيه أو تنبيه إداري", R.drawable.ic_hr_notes));
        r1.addView(spaceW(7));
        r1.addView(movementCard("نقل", "تغيير شعبة الموظف", R.drawable.ic_hr_people));
        deck.addView(r1);

        deck.addView(space(7));
        LinearLayout r2 = horizontal();
        r2.addView(movementCard("تنسيب", "تكليف مؤقت", R.drawable.ic_hr_admin));
        r2.addView(spaceW(7));
        r2.addView(movementCard("إنهاء تنسيب", "إغلاق التكليف المؤقت", R.drawable.ic_hr_quality));
        deck.addView(r2);
        return deck;
    }

    private LinearLayout movementCard(String title, String subtitle, int iconRes) {
        boolean active = title.equals(currentMovement);
        int accent = movementColor(title);
        LinearLayout c = card(14);
        c.setPadding(dp(9), dp(9), dp(9), dp(9));
        c.setMinimumHeight(dp(88));
        c.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c.setBackground(gradient(active ? Color.rgb(24, 39, 101) : Color.rgb(16, 27, 78), Color.rgb(8, 17, 48), 14, active ? accent : Color.rgb(38, 52, 100)));
        c.setOnClickListener(v -> {
            currentMovement = title;
            showManagerNotes();
        });
        LinearLayout top = horizontal();
        top.addView(iconView(iconRes, active ? Color.WHITE : accent, active ? accent : Color.rgb(22, 34, 83), dp(34)));
        top.addView(spaceW(7));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 13, active ? Color.WHITE : TEXT, true));
        labels.addView(text(subtitle, 9, active ? Color.rgb(220, 232, 255) : MUTED, false));
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c.addView(top);
        c.addView(space(8));
        c.addView(text(active ? "محدد الآن" : "اختيار", 10, active ? accent : MUTED, true));
        return c;
    }

    private LinearLayout notesToolbar() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.addView(text("سجل الملاحظات", 18, TEXT, true));
        box.addView(space(4));
        box.addView(text("يتم حفظ الملاحظات محليًا، مع تنظيم العرض حسب نوع الجهاز والصلاحيات المحددة في R2.11.0.", 11, MUTED, false));
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
        c.setBackground(gradient(Color.rgb(13, 24, 68), Color.rgb(8, 17, 48), 18, "new".equals(n.status) ? movementColor(n.type) : BORDER));
        LinearLayout top = horizontal();
        top.addView(badge(n.type, movementColor(n.type)));
        top.addView(spaceW(8));
        top.addView(badge("new".equals(n.status) ? "جديد" : "تمت المراجعة", "new".equals(n.status) ? ORANGE : GREEN));
        top.addView(spaceW(8));
        top.addView(lightBadge(n.id, Color.rgb(27, 43, 102)));
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

    private void checkForAppUpdate() {
        if (isCheckingUpdate || isDownloadingUpdate) return;
        isCheckingUpdate = true;
        Toast.makeText(this, "جاري فحص آخر تحديث...", Toast.LENGTH_SHORT).show();
        showUpdateCenter();
        io.execute(() -> {
            try {
                String json = downloadUpdateManifest();
                UpdateInfo info = parseUpdateInfo(json);
                ui.post(() -> {
                    latestUpdate = info;
                    isCheckingUpdate = false;
                    if (info.versionCode > APP_VERSION_CODE) {
                        Toast.makeText(this, "يوجد تحديث جديد: " + info.versionName, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "لا يوجد تحديث أحدث حاليًا", Toast.LENGTH_SHORT).show();
                    }
                    showUpdateCenter();
                });
            } catch (Exception ex) {
                ui.post(() -> {
                    isCheckingUpdate = false;
                    Toast.makeText(this, "فشل فحص التحديث: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    showUpdateCenter();
                });
            }
        });
    }

    private String downloadUpdateManifest() throws Exception {
        try {
            return downloadText(UPDATE_MANIFEST_URL + "?t=" + System.currentTimeMillis());
        } catch (Exception first) {
            return downloadText(UPDATE_MANIFEST_URL_FALLBACK + "?t=" + System.currentTimeMillis());
        }
    }

    private UpdateInfo parseUpdateInfo(String raw) throws Exception {
        JSONObject o = new JSONObject(raw);
        UpdateInfo info = new UpdateInfo();
        info.versionCode = o.optInt("versionCode", 0);
        info.versionName = o.optString("versionName", "");
        info.apkUrl = o.optString("apkUrl", "");
        info.artifactName = o.optString("artifactName", "");
        info.notes = o.optString("notes", "");
        if (info.versionName.length() == 0) info.versionName = "غير محدد";
        return info;
    }

    private void downloadAndInstallUpdate(UpdateInfo info) {
        if (info == null || info.apkUrl.length() == 0 || isDownloadingUpdate) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(this, "اسمح للتطبيق بتثبيت التحديثات ثم اضغط تحميل وتثبيت مرة أخرى", Toast.LENGTH_LONG).show();
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            settings.setData(Uri.parse("package:" + getPackageName()));
            startActivity(settings);
            return;
        }

        isDownloadingUpdate = true;
        Toast.makeText(this, "جاري تحميل التحديث...", Toast.LENGTH_SHORT).show();
        showUpdateCenter();
        io.execute(() -> {
            try {
                File dir = new File(getFilesDir(), "updates");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("تعذر إنشاء مجلد التحديثات");
                File apk = new File(dir, "HRNativeAndroid-" + info.versionName.replace(" ", "-") + ".apk");
                downloadFile(info.apkUrl, apk);
                ui.post(() -> {
                    isDownloadingUpdate = false;
                    Toast.makeText(this, "تم التحميل، افتح شاشة التثبيت الآن", Toast.LENGTH_SHORT).show();
                    openApkInstaller(apk);
                    showUpdateCenter();
                });
            } catch (Exception ex) {
                ui.post(() -> {
                    isDownloadingUpdate = false;
                    Toast.makeText(this, "فشل تحميل التحديث: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    showUpdateCenter();
                });
            }
        });
    }

    private void downloadFile(String urlText, File target) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlText);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Accept", "application/vnd.android.package-archive,*/*");
            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (code < 200 || code >= 300) {
                String err = readStream(in);
                throw new Exception("HTTP " + code + " - " + err);
            }
            try (InputStream input = in; FileOutputStream output = new FileOutputStream(target, false)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            if (target.length() < 1024) throw new Exception("ملف APK غير صالح أو فارغ");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void openApkInstaller(File apk) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } catch (Exception ex) {
            Toast.makeText(this, "تعذر فتح شاشة التثبيت: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
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

    static class UpdateInfo {
        int versionCode = 0;
        String versionName = "";
        String apkUrl = "";
        String artifactName = "";
        String notes = "";
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
