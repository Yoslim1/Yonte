# تقرير تسليم إعادة تصميم Yonte — الإصدار 1.4.0

## النتيجة

تم تنفيذ إعادة تصميم أصلية لـ Yonte مستوحاة من مبادئ التدفق والتنظيم التي تمت مراجعتها في Knote، مع الحفاظ على هوية Yonte المحلية وواجهتها وطبقاتها المعمارية. لم يتم نسخ كود أو أصول أو ألوان أو نصوص من Knote. تم تثبيت التغييرات في commit `92e29ae` وtag `v1.4.0` في [المستودع الخاص بالمصدر](https://github.com/Yoslim1/Yonte).

## ما تم تنفيذه

| المجال | التنفيذ |
|---|---|
| المحرر | تدفق أكثر هدوءاً للعنوان والنص، شريط أدوات أفقي قابل للسحب، وأفعال نصية حقيقية فقط: عنوان، نقطة، مهمة، وفاصل. |
| الحفظ التلقائي | UUID ثابت للمسودة الجديدة قبل أول حفظ غير متزامن، debounce للحفظ، وحفظ نهائي عند Back أو الإغلاق أو إزالة الشاشة، مع تجاهل الملاحظة الفارغة. |
| الإعدادات | صفحة كاملة بدلاً من AlertDialog، مع قائمة أقسام وصفحات Appearance وData & backup وUpdates ومسار رجوع متوقع. |
| العزل | وحدة `:feature:settings` مستقلة تعتمد على Core فقط؛ app هو Composition Root، ولا يوجد اعتماد بين notes وsettings. |
| السطح الرئيسي | الإبقاء على drawer من جهة البداية وفق RTL، البحث، الوسوم، pinned/recent، وحالة List/Grid الفعلية الموجودة مسبقاً. |
| التوثيق والاختبارات | إضافة `docs/KN0TE_UX_ADAPTATION.md` واختبارات وحدة لأفعال المحرر، وتحديث مهارة `yonte-design`. |

## بوابات الجودة

| الفحص | النتيجة |
|---|---|
| `python3 tools/check_architecture.py` | PASS: لا توجد feature-to-feature أو feature/core-to-app edges. |
| `:feature:notes:test` | PASS. |
| `:core:database:test` | PASS. |
| `:core:update:test` | PASS. |
| `:app:assembleDebug` | PASS. |
| `:app:assembleRelease` | PASS. |
| `apksigner verify --verbose` | PASS، موقّع واحد، APK Signature Scheme v2 صالح. |
| مطابقة checksum المحلي وmanifest | PASS. |
| تحقق manifest المنشور ورابط APK | PASS، HTTP 200 للرابط العام. |
| محاكي أو جهاز فعلي | غير متاح في بيئة التنفيذ؛ لم يتم الادعاء باختبار تفاعل UI على جهاز. |

## الإصدار والقناة العامة

تم نشر [GitHub Release v1.4.0](https://github.com/Yoslim1/Yonte-updates/releases/tag/v1.4.0) في المستودع العام المخصص للـ APK والـ manifest فقط. يطابق `update.json` الإصدار `versionCode: 5` و`versionName: 1.4.0`.

| العنصر | القيمة |
|---|---|
| APK | `Yonte-v1.4.0.apk` |
| SHA-256 | `b27a849cd5f8abf3d22cafa8e8f871ff212b25ddd71997fecb48fe9250c1f88d` |
| شهادة التوقيع SHA-256 | `3679ef199cd7c2471a8ccd128bbc428ff12575b74c1fa4d222760692f2ddab23` |
| رابط التنزيل | `https://github.com/Yoslim1/Yonte-updates/releases/download/v1.4.0/Yonte-v1.4.0.apk` |
| commit مستودع التحديث | `a81253f` |

## الأرشيف

تم إنشاء أرشيف مصدر نظيف من tag `v1.4.0` باسم `Yonte-1.4.0-source.zip`. تم التحقق من عدم احتوائه على `.git` أو `build` أو `local.properties` أو مفاتيح وproperties التوقيع.
