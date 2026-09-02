# Yonte 1.6.0 — Notes & Tasks delivery

Status: HISTORICAL
Last reviewed: 2026-09-02

## القرار التصميمي

الملفان المرفقان صُمما لـ Knote، ولذلك استُخدما كمرجع لفهم التدفق والهرمية البصرية فقط. بقي اسم المنتج **Yonte**، وبقي package `com.yonte.app`، وبقيت الخصوصية المحلية وبنية الوحدات كما هي. لم تُنقل أي ألوان أو شعارات أو نصوص أو أصول أو كود من Knote.

## ما تم تحويله إلى Yonte

| مبدأ في المرجع | تطبيق Yonte الأصلي |
|---|---|
| مساحة كتابة هادئة مع حفظ تلقائي | محرر borderless بعنوان كبير، نص واسع، scroll موحد، `imePadding`، ومؤشر Saved locally/Saving. |
| إضافة سريعة من أسفل الشاشة | Quick Add Bottom Sheet خاص بـ Yonte يتيح اختيار Note أو Task من نفس الشاشة. |
| المهام ذات حالة إنجاز | يبدأ Task حالياً كسطر checkable فعلي داخل الملاحظة (`- [ ]`) بدلاً من عرض قاعدة بيانات مهام غير منفذة. هذا يحافظ على صدق المنتج إلى أن يتم اعتماد TaskEntity وترحيله باختبارات مستقلة. |
| بحث وفلاتر وتقسيم المحتوى | بحث هادئ، وسوم، Pinned/Recent، وList/Grid حقيقيان داخل Home. |
| إعدادات منظمة | صفحة إعدادات كاملة مستقلة بأقسام وأيقونات، دون ربط feature-to-feature. |

## بوابات الجودة

تم تشغيل `python3 tools/check_architecture.py`، اختبارات `:feature:notes:test` و`:core:database:test` و`:core:update:test`، وبناء `:app:assembleDebug` و`:app:assembleRelease`. تم التحقق من APK بتوقيع واحد وبصمة الشهادة الثابتة، ثم طابقت قناة التحديث العامة manifest مع APK المنشور.

لا يوجد جهاز أو محاكي Android متصل ببيئة التنفيذ. لذلك يظل تثبيت APK على هاتف فعلي خطوة ضرورية للحكم النهائي على الإيقاع البصري، موضع Bottom Sheet، وتفاصيل RTL على مقاسات مختلفة.

## الإصدار

الإصدار هو `versionCode 7` و`versionName 1.6.0`. رابط [GitHub Release v1.6.0](https://github.com/Yoslim1/Yonte-updates/releases/tag/v1.6.0) موجود في مستودع التحديث العام، بينما المصدر في [Yoslim1/Yonte](https://github.com/Yoslim1/Yonte).
