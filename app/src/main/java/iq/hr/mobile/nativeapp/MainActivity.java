package iq.hr.mobile.nativeapp;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(245, 247, 251);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(24, 33, 48);
    private static final int MUTED = Color.rgb(105, 117, 135);
    private static final int PRIMARY = Color.rgb(32, 97, 202);
    private static final int GREEN = Color.rgb(20, 150, 96);
    private static final int ORANGE = Color.rgb(220, 132, 28);
    private static final int PURPLE = Color.rgb(120, 82, 210);
    private static final int RED = Color.rgb(205, 61, 72);
    private static final int BORDER = Color.rgb(225, 231, 241);

    private LinearLayout root;
    private String currentRole = "system_admin";
    private String currentMovement = "ملاحظة";
    private Employee selectedEmployee;
    private final List<Employee> employees = new ArrayList<>();
    private final List<ManagerNote> notes = new ArrayList<>();
    private LinearLayout notesContainer;
    private String currentFilter = "الكل";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        seedData();
        showHome();
    }

    private void seedData() {
        employees.add(new Employee("1001", "أحمد محمد حسن", "شعبة الموارد البشرية", "دائم"));
        employees.add(new Employee("1002", "علي حسين كاظم", "شعبة الحسابات", "دائم"));
        employees.add(new Employee("1003", "سجاد حسن جبار", "شعبة تكنولوجيا المعلومات", "عقد"));
        employees.add(new Employee("1004", "زهراء عبد الكريم", "شعبة التخطيط", "دائم"));
        employees.add(new Employee("1005", "مصطفى صالح مهدي", "شعبة المتابعة", "عقد"));

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

    private void showHome() {
        baseScreen();

        LinearLayout hero = card(18);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView title = text("نظام الموارد البشرية", 24, TEXT, true);
        TextView subtitle = text("نسخة Android Native R2.0.1 — تصميم أولي مطابق لترتيب النسخة الحالية", 13, MUTED, false);
        hero.addView(title);
        hero.addView(space(6));
        hero.addView(subtitle);
        hero.addView(space(14));

        LinearLayout roleBar = horizontal();
        roleBar.addView(roleButton("مسؤول النظام", "system_admin"));
        roleBar.addView(spaceW(8));
        roleBar.addView(roleButton("مدير الموارد البشرية", "hr_manager"));
        hero.addView(roleBar);
        root.addView(hero);

        root.addView(space(14));
        root.addView(sectionTitle("لوحة البرامج"));

        LinearLayout grid1 = horizontal();
        grid1.addView(dashboardCard("القائمة", "بحث مباشر وبطاقة موظف", PRIMARY));
        grid1.addView(spaceW(10));
        grid1.addView(dashboardCard("ملاحظات المدير", "نقل، تنسيب، أرشفة", PURPLE));
        root.addView(grid1);

        root.addView(space(10));
        LinearLayout grid2 = horizontal();
        grid2.addView(dashboardCard("التقارير", "PDF وتقارير الشعب", GREEN));
        grid2.addView(spaceW(10));
        grid2.addView(dashboardCard("حالة النظام", "مزامنة وتشخيص", ORANGE));
        root.addView(grid2);

        root.addView(space(16));
        Button notesBtn = primaryButton("فتح ملاحظات المدير");
        notesBtn.setOnClickListener(v -> showManagerNotes());
        root.addView(notesBtn);

        root.addView(space(12));
        TextView footer = text("هذه حزمة بداية Native. الربط الحقيقي مع GitHub / Google Sheet / Firebase يتم في مراحل R2 التالية.", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
    }

    private void showManagerNotes() {
        baseScreen();

        LinearLayout header = card(18);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView title = text("ملاحظات مدير الموارد البشرية", 22, TEXT, true);
        TextView subtitle = text("متابعة النقل، التنسيب، إنهاء التنسيب، والملاحظات الإدارية", 13, MUTED, false);
        header.addView(title);
        header.addView(space(5));
        header.addView(subtitle);
        header.addView(space(14));

        LinearLayout row = horizontal();
        row.addView(badge(roleLabel(), currentRole.equals("system_admin") ? PRIMARY : PURPLE));
        row.addView(spaceW(8));
        row.addView(badge("R2.0.1 Native", GREEN));
        header.addView(row);
        root.addView(header);

        root.addView(space(12));
        root.addView(statsPanel());
        root.addView(space(12));

        if (currentRole.equals("hr_manager")) {
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
        form.addView(text("ابحث عن الموظف بكتابة حرفين أو أكثر، لا تظهر الأسماء عند الضغط فقط.", 12, MUTED, false));
        form.addView(space(12));

        EditText search = editText("اكتب اسم الموظف للبحث...");
        form.addView(search);
        form.addView(space(8));

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        form.addView(results);

        TextView selected = text("لم يتم اختيار موظف", 13, MUTED, false);
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
            String text = note.getText().toString().trim();
            notes.add(0, new ManagerNote("MN-" + System.currentTimeMillis(), currentMovement, selectedEmployee.name,
                    selectedEmployee.branch, currentMovement.equals("ملاحظة") ? "" : "الشعبة الجديدة", text, "new", now()));
            Toast.makeText(this, "تم إرسال الملاحظة محليًا في النموذج الأولي", Toast.LENGTH_SHORT).show();
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
                String q = s.toString().trim();
                results.removeAllViews();
                if (q.length() < 2) {
                    results.addView(helperLine("اكتب حرفين أو أكثر لعرض النتائج"));
                    return;
                }
                int hits = 0;
                for (Employee e : employees) {
                    if (e.name.contains(q) || e.branch.contains(q) || e.id.contains(q)) {
                        hits++;
                        TextView item = resultItem(e.name + "\n" + e.branch + " — " + e.type);
                        item.setOnClickListener(v -> {
                            selectedEmployee = e;
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
                if (hits == 0) results.addView(helperLine("لا توجد نتائج مطابقة"));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return form;
    }

    private LinearLayout notesToolbar() {
        LinearLayout box = card(18);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.addView(text("سجل الملاحظات", 18, TEXT, true));
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
            if (currentRole.equals("hr_manager") && "reviewed".equals(n.status)) continue;
            notesContainer.addView(noteCard(n));
            notesContainer.addView(space(10));
            shown++;
        }
        if (shown == 0) {
            LinearLayout empty = card(18);
            empty.setPadding(dp(18), dp(18), dp(18), dp(18));
            TextView t = text("لا توجد ملاحظات ضمن هذا الفلتر", 14, MUTED, false);
            t.setGravity(Gravity.CENTER);
            empty.addView(t);
            notesContainer.addView(empty);
        }
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
        if (currentRole.equals("system_admin") && "new".equals(n.status)) {
            c.addView(space(12));
            Button reviewed = primaryButton("تمت المراجعة");
            reviewed.setOnClickListener(v -> {
                n.status = "reviewed";
                Toast.makeText(this, "تمت المراجعة ونقلت إلى الأرشيف", Toast.LENGTH_SHORT).show();
                showManagerNotes();
            });
            c.addView(reviewed);
        }
        return c;
    }

    private Button roleButton(String label, String role) {
        Button b = pillButton(label, currentRole.equals(role) ? PRIMARY : Color.WHITE, currentRole.equals(role) ? Color.WHITE : TEXT);
        b.setOnClickListener(v -> {
            currentRole = role;
            Toast.makeText(this, "تم اختيار: " + label, Toast.LENGTH_SHORT).show();
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
        if ("ملاحظات المدير".equals(title)) {
            c.setOnClickListener(v -> showManagerNotes());
        }
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

    private Button primaryButton(String s) {
        return pillButton(s, PRIMARY, Color.WHITE);
    }

    private Button outlineButton(String s) {
        return pillButton(s, Color.WHITE, PRIMARY);
    }

    private Button chipButton(String s, boolean active) {
        return pillButton(s, active ? PRIMARY : Color.WHITE, active ? Color.WHITE : TEXT);
    }

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

    private String roleLabel() {
        return currentRole.equals("system_admin") ? "مسؤول النظام" : "مدير الموارد البشرية";
    }

    private String safe(String v) { return v == null || v.length() == 0 ? "-" : v; }

    private String now() {
        return new SimpleDateFormat("yyyy/MM/dd - hh:mm a", new Locale("ar", "IQ")).format(new Date());
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class Employee {
        final String id;
        final String name;
        final String branch;
        final String type;
        Employee(String id, String name, String branch, String type) {
            this.id = id; this.name = name; this.branch = branch; this.type = type;
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
