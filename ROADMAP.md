# خطة Yonte المعمارية والمنتجية الشاملة

**آخر تحديث:** أغسطس 2026
**الحالة عند كتابة هذا المستند:** 15 commit، 1737 سطر كود، موديول واحد فعّال فعليًا (`feature:notes`)، قاعدة بيانات بإصدار Schema واحد (`version = 1`)، كيانة واحدة (`NoteEntity`).

هذا المستند مرجع دائم — أي قرار معماري أو منتجي جديد يُضاف هنا، مش يتقال في محادثة وينسى. الهدف: بعد سنة، أي مهندس (أنا، أنت، أو حد جديد) يفتح الملف ده ويفهم **ليه** الهيكل شكله كده، مش بس **إيه** هو.

---

## جدول المحتويات

1. [الرؤية والمراحل](#1-الرؤية-والمراحل)
2. [المبادئ غير القابلة للتفاوض](#2-المبادئ-غير-القابلة-للتفاوض)
3. [هيكل الموديولات — الحالي والمستهدف](#3-هيكل-الموديولات--الحالي-والمستهدف)
4. [قانون توزيع الملفات (Feature-Level File Law)](#4-قانون-توزيع-الملفات-feature-level-file-law)
5. [نظام التصميم (Design System)](#5-نظام-التصميم-design-system)
6. [طبقة البيانات (Data Layer) — الاستراتيجية الكاملة](#6-طبقة-البيانات-data-layer--الاستراتيجية-الكاملة)
7. [الأمان والنسخ الاحتياطي](#7-الأمان-والنسخ-الاحتياطي)
8. [إدارة الحالة (State Management) والـ Navigation](#8-إدارة-الحالة-state-management-والـ-navigation)
9. [استراتيجية الاختبار](#9-استراتيجية-الاختبار)
10. [خطة الـ CI/CD التطورية](#10-خطة-الـ-cicd-التطورية)
11. [خارطة الطريق المرحلية بالتفصيل](#11-خارطة-الطريق-المرحلية-بالتفصيل)
12. [Definition of Done — لكل ميزة جديدة](#12-definition-of-done--لكل-ميزة-جديدة)

---

## 1) الرؤية والمراحل

Yonte مش "تطبيق ملاحظات" — هو **مساحة عمل شخصية واحدة** بتتوسع بالتدريج:
المرحلة 1 (الآن)       المرحلة 2            المرحلة 3            المرحلة 4
┌──────────┐          ┌──────────┐         ┌──────────┐         ┌──────────┐
│  Notes   │  ───►    │  + Tasks │  ───►   │ + Habits │  ───►   │ + Finance│
│  (نضّج)  │          │          │         │          │         │          │
└──────────┘          └──────────┘         └──────────┘         └──────────┘
**القاعدة الذهبية:** كل مرحلة جديدة **لازم متضربش الاستقرار بتاع اللي قبلها**. ده مش شعار — ده بيترجم لقرارات فعلية في القسم 6 (قاعدة بيانات واحدة، migrations إضافية بس، مفيش destructive changes).

---

## 2) المبادئ غير القابلة للتفاوض

هذه قواعد ثابتة، أي قرار تقني لازم يتماشى معاها، مش العكس:

1. **مفيش ملف بيعمل أكتر من مسؤولية واحدة.** لو ملف عدّى ~150 سطر وفيه أكتر من `@Composable` top-level واحد أو أكتر من class واحدة، **لازم يتفصل**.
2. **مفيش feature بيستورد feature تانية مباشرة.** أي تواصل بين ميزتين (مثلاً "مهمة مرتبطة بملاحظة") يعدّي عبر `core` module (contract/interface)، مش import مباشر. (`check_architecture.py` الموجود بالفعل بيفرض ده تلقائيًا — هنوسّعه.)
3. **قاعدة بيانات واحدة (`YonteDatabase`)، مش قاعدة لكل ميزة.** التوسع بيبقى schema إضافي (entities جديدة + migrations)، مش قواعد بيانات منفصلة — عشان نقدر نعمل علاقات بين الميزات (مهمة مرتبطة بعادة، مصروف مرتبط بملاحظة) من غير تعقيد.
4. **الأمان مستوى واحد للكل.** لو الملاحظات مشفّرة، المهام والعادات والبيانات المالية **بنفس مستوى التشفير من أول يوم**، مش "نضيفه بعدين".
5. **العربي مش "ترجمة إضافية" — هو مواطن أول درجة.** RTL، الخطوط، التقويم، تنسيق الأرقام — بيتصمم من الأول لعربي+إنجليزي مع بعض، مش إنجليزي الأول وعربي "يتظبط بعدين" (الغلطة اللي حصلت في Knote بالظبط).
6. **الأداء قرار تصميم، مش تحسين لاحق.** أي شاشة قائمة (notes list, tasks list) لازم تستخدم lazy loading + pagination من أول commit ليها، مش نضيفها لما تبطأ.

---

## 3) هيكل الموديولات — الحالي والمستهدف

### الوضع الحالي (بعد الفحص المباشر):
:app
:core:database      (NoteEntity, NoteDao, YonteDatabase, NoteRepository)
:core:security       (EncryptionManager)
:core:backup          (BackupService)
:core:navigation      (Navigators)
:core:designsystem   (YonteTheme — كل حاجة في ملف واحد)
:core:update          (UpdateService)
:feature:notes        (6 ملفات، فيها NotesHomeV2.kt الكبير)
:feature:settings    (SettingsRoute.kt)
### الهيكل المستهدف مع التوسع (المراحل 2-4):
:app
:core:database
├── notes/       (NoteEntity, NoteDao, NoteRepository — موجود، هيتحرك جوه فولدر)
├── tasks/         (TaskEntity, TaskDao, TaskRepository — مرحلة 2)
├── habits/        (HabitEntity, HabitLogEntity, HabitDao — مرحلة 3)
├── finance/       (ExpenseEntity, CategoryEntity, FinanceDao — مرحلة 4)
└── YonteDatabase.kt   (نقطة التجميع الوحيدة، كل الـ @Database entities هنا)
:core:security        (زي ما هو — يُستخدم من كل الميزات)
:core:backup           (يتوسع تدريجيًا: envelope مرن يحتوي كل الدومينز حسب الإصدار)
:core:navigation
:core:designsystem
├── color/         (YonteColors.kt)
├── typography/    (YonteTypography.kt)
├── shape/          (YonteShapes.kt)
├── spacing/        (YonteSpacing.kt — جديد، مذكور تحت في القسم 5)
└── YonteTheme.kt   (بس التجميع، مفيهوش تعريفات)
:feature:notes
:feature:tasks         (مرحلة 2 — جديد)
:feature:habits        (مرحلة 3 — جديد)
:feature:finance       (مرحلة 4 — جديد)
:feature:settings
:feature:home           (مرحلة 2 — شاشة رئيسية موحّدة تجمع الكل، بدل ما notes تبقى الشاشة الرئيسية دايمًا)
**ملاحظة مهمة:** موديول `feature` جديد لكل دومين كبير (tasks, habits, finance)، لكن `core:database` **موديول واحد بيتقسم لفولدرات داخلية**، مش موديول منفصل لكل دومين. السبب: الـ Room database نفسها object واحد (`@Database` annotation)، تقسيمها لموديولات منفصلة بيعقّد الـ migrations والعلاقات من غير فايدة حقيقية دلوقتي بحجم المشروع ده.

---

## 4) قانون توزيع الملفات (Feature-Level File Law)

القاعدة دي بترد مباشرة على مشكلة `NotesHomeV2.kt` (376 سطر، 8 مسؤوليات):

### القاعدة:
- **composable واحد top-level لكل ملف**, والـ helper composables الصغيرة (زي `SectionHeader`, `EmptyWorkspace`) بتتحط في ملف `<Feature>Components.kt` منفصل لو مستخدمة في أكتر من شاشة، أو تتحط جوه ملف الشاشة **بس لو أقل من 3 helpers وكل واحد أقل من 20 سطر**.
- **الحد الأقصى المرجعي لملف الشاشة نفسها: 150 سطر.** لو عدّى، ده إشارة إن الشاشة بتعمل حاجتين (مثلاً "عرض + بحث" → يتفصلوا).
- **الـ state (UiState) في ملف منفصل** عن الـ Composable وعن الـ ViewModel (نمط: `NotesScreen.kt` + `NotesUiState.kt` + `NotesViewModel.kt`).
- **تسمية بدون أرقام إصدار** (`V2`, `New`, `Old`) — لو محتاج تجربة نسخة جديدة، استخدم branch أو feature flag، مش اسم الملف. لو فيه نسخة قديمة، تتمسح فورًا بعد الدمج، مش تتسيب.

### تطبيق فوري على `NotesHomeV2.kt` (أول تنفيذ عملي للقاعدة، خطوة تالية بعد الـ CI):
NotesHomeV2.kt (376 سطر)  →
NotesHomeScreen.kt          (الشاشة الرئيسية بس)
NoteCard.kt                  (كان NoteCardV2)
QuickAddChoice.kt
NotesSearchField.kt          (كان SearchField — اسم عام جدًا، هيتلخبط مع settings)
NotesTagStrip.kt             (كان TagStrip)
NotesViewModeToggle.kt       (كان ViewModeToggle)
NotesSectionHeader.kt
NotesEmptyState.kt           (كان EmptyWorkspace)
---

## 5) نظام التصميم (Design System)

### الوضع الحالي (بعد الفحص):
`YonteTheme.kt` (92 سطر) فيه: الألوان + الأشكال + التايبوجرافي + الـ Composable نفسه — 4 مسؤوليات في ملف واحد. مقبول لحجم المشروع دلوقتي، **لكن هيتكسر بسرعة مع أي ميزة جديدة** (كل ميزة جديدة هتحتاج ألوان دلالية خاصة بيها — مثلاً لون "متأخر" للمهام، لون "streak" للعادات، لون "سالب/موجب" للمصروفات).

### القرار: فصل الـ Design Tokens من أول التوسع القادم (مش دلوقتي فورًا، لكن قبل مرحلة Tasks):

| الملف | المسؤولية |
|---|---|
| `YonteColors.kt` | الألوان الأساسية + الدلالية (Semantic: success, warning, danger, overdue) |
| `YonteTypography.kt` | التايبوجرافي — **لازم يتفحص مع خط عربي حقيقي** (مش بس يتأكد إن النص العربي بيتعرض، لازم يتأكد إن الـ line-height والـ letter-spacing مظبوطين للعربي، لأن أرقام زي دي بتختلف عن اللاتيني) |
| `YonteShapes.kt` | الأشكال |
| `YonteSpacing.kt` | **جديد** — نظام مسافات موحّد (4dp/8dp/12dp/16dp/24dp/32dp scale) بدل أرقام مباشرة متفرقة في كل شاشة — بيضمن التناسق البصري عبر كل الميزات |
| `YonteMotion.kt` | **جديد، مرحلة لاحقة** — مدد وانحناءات الحركة الموحّدة (transitions بين الشاشات، animations) — عشان التطبيق "يحس" إنه نفس المنتج مش شاشات متفرقة |

### مبادئ الـ UX تحديدًا:
1. **RTL أولوية تصميم فعلية**: أي أيقونة اتجاهية (سهم رجوع، ترتيب) لازم تتفحص بصريًا في الاتجاهين، مش بس تعتمد على `LayoutDirection` التلقائي.
2. **حركة (Motion) متسقة**: نفس نوع الانتقال بين كل الشاشات المتشابهة (فتح ملاحظة = فتح مهمة = فتح عادة، من ناحية الحركة).
3. **حالة فارغة (Empty State) لكل قائمة**: مش نص عادي — رسمة/أيقونة + جملة توضيحية + call-to-action (موجود جزئيًا في `EmptyWorkspace`، هيتعمم كنمط قياسي لكل ميزة جديدة).
4. **Loading/Error states موحّدة**: مكوّن واحد `YonteLoadingIndicator` و`YonteErrorState` يُستخدم في كل مكان، مش كل شاشة بتعمل تصميمها الخاص.

---

## 6) طبقة البيانات (Data Layer) — الاستراتيجية الكاملة

### الوضع الحالي (بعد الفحص المباشر):

```kotlin
@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
abstract class YonteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    ...
    .fallbackToDestructiveMigrationOnDowngrade()
ملاحظات على الكود الحالي:
✅ exportSchema = true — صح، نفس ممارسة Knote الجيدة.
✅ fallbackToDestructiveMigrationOnDowngrade() — مش fallbackToDestructiveMigration() العادي. يعني الحماية دي بس لحالة الـ downgrade (نادرة جدًا)، مش الـ upgrade العادي. قرار صحيح تمامًا، ومطابق للدرس اللي اتعلمناه من Knote.
⚠️ جدول الـ FTS5 (notes_fts) بيتعمل بـ execSQL يدوي جوّه onCreate callback، مش عبر Room's @Fts4/@Fts5 entity annotation. ده شغال، لكن Room مش عارف بوجود الجدول ده رسميًا — أي migration مستقبلية تلمس الملاحظات لازم تتذكر تتعامل مع notes_fts يدويًا برضو، لأن Room مش هيعملها تلقائي.
استراتيجية التوسع (لكل مرحلة قادمة):
القاعدة الأساسية: زيادة version + migration صريح لكل تغيير schema — نفس انضباط Knote بالظبط، من أول يوم مش بعد ما نتعلم الدرس بالطريقة الصعبة.
مرحلة 2 — Tasks:
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String? = null,
    val dueAt: Long? = null,
    val isCompleted: Boolean = false,
    val linkedNoteId: String? = null,  // علاقة اختيارية بالملاحظات — FK مش إجباري
    val createdAt: Long,
    val updatedAt: Long,
)
version = 2, migration MIGRATION_1_2 بيضيف جدول tasks بس، مفيش لمس لجدول notes.
لو محتاجين FTS للمهام كمان، هنعمل جدول FTS منفصل (tasks_fts) مش نحاول نشارك notes_fts — كل دومين له جدول بحث خاص بيه، أوضح وأسهل صيانة.
مرحلة 3 — Habits:
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetPerWeek: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val loggedAt: Long,
)
version = 3. أول مرة نستخدم ForeignKey فعلي في المشروع — قرار مهم: نستخدمه هنا لأن العلاقة "log بيخص عادة" علاقة صارمة (log بدون عادة مالوش معنى)، عكس علاقة "مهمة مرتبطة بملاحظة" اللي هي اختيارية وأضعف.
مرحلة 4 — Finance (أعلى حساسية أمنية بعد كلمات المرور):
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amountMinorUnits: Long,   // بالقروش/السنتات — تجنب Float للمبالغ المالية تمامًا
    val currencyCode: String,      // ISO 4217
    val categoryId: String,
    val note: String? = null,
    val occurredAt: Long,
)
قاعدة صارمة: أي قيمة مالية Long (أصغر وحدة عملة) أبدًا مش Float/Double — أخطاء التقريب في العملات مشكلة حقيقية وشائعة، ونمنعها من الـ type نفسه.
المرحلة دي لازم تراجعة أمنية كاملة زي KeyManager بتاع Knote بالظبط قبل ما تتفعل — مش ميزة عادية.
سياسة الـ Migration (نفس نمط MIGRATION_POLICY.md بتاع Knote، لكن مكتوبة من الأول مش بعد المشكلة):
أي تغيير schema = migration صريح + version جديد. ممنوع fallbackToDestructiveMigration() العادي نهائيًا.
كل migration بيتحط في ملف منفصل MigrationsNtoM.kt جوّه core:database (مش كلهم في YonteDatabase.kt نفسه — بيرجع لقانون توزيع الملفات في القسم 4).
Migration test (MigrationTestHelper) إلزامي من أول migration حقيقية — مش نأجله زي ما حصل في Knote.
7) الأمان والنسخ الاحتياطي
core:security — الوضع الحالي:
EncryptionManager.kt (50 سطر) — بداية بسيطة. مع دخول مرحلة Finance، لازم يتوسع لنفس مستوى نضج KeyManager بتاع Knote:
AndroidKeyStore-backed key wrapping
StrongBox fallback على الأجهزة اللي بتدعمها
Biometric-gated unlock اختياري
Instrumented tests من أول يوم (مش نسيبها فاضية زي دلوقتي)
core:backup — نقطة قوة موجودة بالفعل:
الـ README بيقول "Versioned encrypted backup envelope" — ده قرار ممتاز ومُطبّق من الأول (عكس Knote اللي احتاج يبني backup versioning لاحقًا). لازم نتأكد إن كل دومين جديد (tasks, habits, finance) بيتضاف للـ envelope schema كحقل اختياري، عشان نسخة احتياطية قديمة (notes بس) تفضل قابلة للاستعادة حتى بعد ما نضيف دومينز جديدة.
8) إدارة الحالة (State Management) والـ Navigation
نمط موحّد لكل ميزة: <Feature>ViewModel + <Feature>UiState (sealed interface أو data class مع isLoading/error/data) — بنفس النمط الموجود في NotesViewModel.kt حاليًا، يتعمم على كل ميزة جديدة.
core:navigation: مع دخول كل ميزة جديدة، الـ navigation graph هيكبر — لازم يتقسم لـ NotesNavigation.kt, TasksNavigation.kt منفصلين بدل ملف واحد ضخم، كل ميزة بتسجّل الـ routes بتاعتها بنفسها (نفس فلسفة الـ feature isolation).
9) استراتيجية الاختبار
النوع
الهدف
أين
Unit tests
منطق الأعمال الصرف (repositories، formatters، validators)
كل موديول core:* وfeature:*
Instrumented tests
أي حاجة بتلمس Android framework فعليًا (تشفير حقيقي، قاعدة بيانات حقيقية)
core:security, core:database (migrations), core:backup
UI tests (Compose)
شاشات حرجة بس (مش كل شاشة) — Notes list, Editor
feature:notes, لاحقًا feature:tasks
قاعدة التغطية: أي core module جديد (خصوصًا security/backup/database) ممنوع يتدمج من غير test واحد على الأقل يغطي الـ happy path + حالة فشل واحدة. مش نسبة تغطية محددة، لكن "صفر اختبارات" ممنوع تمامًا لموديولات الأمان والبيانات.
10) خطة الـ CI/CD التطورية
المرحلة
الإضافة
دلوقتي (خطوة متفق عليها)
:feature:notes:test + خطوة lint في android.yml
قبل مرحلة Tasks
تشغيل تلقائي لكل موديول (./gradlew test عام بدل تعداد يدوي لكل موديول — يمنع نسيان موديول جديد زي ما حصل مع feature:notes)
قبل مرحلة Finance
Instrumented tests على emulator (نفس درس Knote — الأمان محتاج تحقق حقيقي مش نظري)
عند أول نشر فعلي
تفعيل signed-release job (موجود بالفعل بس معطّل خلف YONTE_SIGNED_RELEASE_ENABLED) — مع درس Knote محفوظ من الأول: versionCode/versionName هيتولدوا من git tag تلقائيًا من الالتزام الأول، مش نضيفها بعد ما يتكرر نفس البَغ
11) خارطة الطريق المرحلية بالتفصيل
المرحلة 1 — تنضيج Notes (دلوقتي)
[ ] إصلاح android.yml (test coverage + lint)
[ ] تفكيك NotesHomeV2.kt حسب قانون القسم 4
[ ] فصل design tokens (القسم 5) — لو هتضاف ميزة جديدة قريب، لو لأ ممكن تتأجل لحد بداية مرحلة 2
[ ] كتابة أول اختبارات لـ core:security وcore:backup
[ ] قرار الترخيص (لسه معلّق من المحادثة اللي فاتت)
المرحلة 2 — Tasks
[ ] TaskEntity + migration 1→2 + migration test
[ ] :feature:tasks module جديد (بنفس بنية :feature:notes)
[ ] :feature:home — شاشة رئيسية موحّدة (بدل ما notes تبقى نقطة الدخول الوحيدة)
[ ] تحديث core:backup envelope عشان يشمل tasks اختياريًا
المرحلة 3 — Habits
[ ] HabitEntity + HabitLogEntity (أول استخدام فعلي لـ ForeignKey)
[ ] :feature:habits
[ ] تصميم "streak" visualization — محتاج قرار UX منفصل (رسم بياني؟ تقويم؟) قبل التنفيذ
المرحلة 4 — Finance
[ ] مراجعة أمنية كاملة قبل أي كود (زي KeyManager audit بتاع Knote)
[ ] ExpenseEntity + CategoryEntity (قيم Long بس، ممنوع Float)
[ ] :feature:finance
[ ] Instrumented tests إلزامية قبل أي دمج
12) Definition of Done — لكل ميزة جديدة
قبل ما أي ميزة (Tasks, Habits, Finance) تُعتبر "خلصت"، لازم:
[ ] كل composable في ملف منفصل حسب قانون القسم 4
[ ] Design tokens مستخدمة من core:designsystem، مفيش ألوان/مسافات hardcoded في الشاشة
[ ] check_architecture.py عدّى بدون violations
[ ] Migration + migration test (لو فيه تغيير schema)
[ ] Unit test واحد على الأقل للـ repository/business logic
[ ] النص بالكامل عربي+إنجليزي من commit الميزة نفسه (مش "نترجم بعدين" — درس Knote الأهم)
[ ] RTL اتفحص بصريًا (مش بس افتراض إنه شغال)
[ ] Lint عدّى بدون أخطاء جديدة
[ ] android.yml (CI) أخضر بالكامل قبل الدمج لـ main
نهاية المستند. أي قرار جديد يتضاف هنا فورًا وقت اتخاذه، مش يُنسى في محادثة قديمة.
