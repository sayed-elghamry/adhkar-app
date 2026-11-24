# تطبيق أذكار (أندرويد / Capacitor 8)

تطبيق أذكار عربي (RTL) يعمل دون إنترنت: المصحف الشريف (نص مجمع الملك فهد بخط حفص العثماني وترقيم صفحات مصحف المدينة، مع بحث وعلامات)، عدّاد تسبيح، أذكار متنوعة، أذكار الصباح والمساء وبعد الصلاة، وحفظ التقدّم على الهاتف، مع **تذكير كل ساعة من ٥ صباحًا إلى ١١ مساءً** بصوت «اللهم صلّ على محمد ﷺ» يعمل والهاتف مقفل أو في تطبيق آخر.

## كيف يعمل التذكير

| الميزة | التنفيذ |
|---|---|
| تذكير في رأس كل ساعة | `AlarmManager.setExactAndAllowWhileIdle` (يعمل في وضع السكون Doze) يجدول الساعة التالية فقط، وعند وصولها يُظهر الإشعار ويجدول التي بعدها |
| الصوت المخصص | قناة إشعارات خاصة بصوت `res/raw/salawat.mp3` |
| بعد إعادة التشغيل | مستقبل `BOOT_COMPLETED` (وكذلك تحديث التطبيق، تغيير الوقت أو المنطقة الزمنية، ومنح إذن المنبّهات) يعيد الجدولة تلقائيًا |
| توفير البطارية | شاشة «الإعدادات» فيها ثلاث خطوات بحالة ✓ لكل منها: الإشعارات، المنبّهات والتذكيرات، الاستثناء من توفير البطارية، ثم إرشاد حسب نوع الهاتف (شاومي، سامسونج، هواوي، أوبو/ريلمي، فيفو، إنفينكس) وزر لفتح شاشة التشغيل التلقائي |
| النافذة الزمنية | قابلة للتغيير من الإعدادات (الافتراضي ٥:٠٠ ص حتى ١١:٠٠ م، أي ١٩ تذكيرًا يوميًا) |

الكود الأصلي في `android/app/src/main/java/com/adhkar/salawat/reminder/`، والواجهة في `www/index.html`.

## وضع ملف الصوت

1. سمِّ الملف **`salawat.mp3`** (أحرف إنجليزية صغيرة فقط).
2. ضعه مكان الملف المؤقت: `android/app/src/main/res/raw/salawat.mp3`
3. يُفضّل أن يكون قصيرًا (٣ إلى ١٠ ثوانٍ).
4. إذا كان التطبيق مثبتًا من قبل بصوت آخر: أندرويد يحفظ صوت القناة عند أول تثبيت، فإما أن **تحذف التطبيق وتثبّته من جديد**، أو تزيد الرقم `SOUND_VERSION` في `ReminderScheduler.java` بمقدار ١.

## البناء على جهازك

المتطلبات (مرة واحدة):
- **Node.js 20+** و **JDK 21**
- **Android Studio** (الأسهل)، أو Android SDK مع `platforms;android-36` و`build-tools;36.0.0`

```bash
# 1) فك الضغط والدخول إلى المجلد
cd adhkar-app

# 2) تثبيت الحزم ونسخ الواجهة إلى مشروع أندرويد
npm install
npx cap sync android

# 3) بناء APK للتجربة
cd android
./gradlew assembleDebug          # على ويندوز: gradlew.bat assembleDebug
```

الملف الناتج: `android/app/build/outputs/apk/debug/app-debug.apk`

- انسخه إلى الهاتف وافتحه (اسمح بالتثبيت من مصادر غير معروفة)، أو:
  `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- أو افتح المشروع في Android Studio بـ `npx cap open android` ثم ▶ Run.

إن ظهر خطأ «SDK location not found»: أنشئ الملف `android/local.properties` وفيه سطر واحد:
`sdk.dir=C\:\\Users\\اسمك\\AppData\\Local\\Android\\Sdk` (ويندوز) أو `sdk.dir=/Users/اسمك/Library/Android/sdk` (ماك).

### البناء على GitHub بدل جهازك
المشروع فيه `.github/workflows/android.yml`. ارفع المجلد إلى مستودع على GitHub، وسيبني ملف APK تلقائيًا، وتجده في تبويب **Actions ← آخر تشغيل ← Artifacts**.

## بعد التثبيت (مهم مرة واحدة)
افتح التطبيق ← **الإعدادات** ← أكمل الخطوات الثلاث حتى تصبح كلها ✓، ثم نفّذ خطوة هاتفك الإضافية، واضغط «جرّب التنبيه» لسماع الصوت.

## المشاركة لاحقًا أو النشر على Google Play
- **ملف APK للمشاركة:** أنشئ مفتاح توقيع مرة واحدة واحتفظ به جيدًا:
  ```bash
  keytool -genkey -v -keystore adhkar-release.jks -alias adhkar -keyalg RSA -keysize 2048 -validity 10000
  ```
  ثم من Android Studio: Build ← Generate Signed App Bundle / APK.
- **Google Play:** ارفع ملف `.aab` (`./gradlew bundleRelease` بعد إعداد التوقيع). انتبه إلى سياسات Play:
  - إذن `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` يحتاج تبريرًا؛ يمكن حذفه من `AndroidManifest.xml`، والتطبيق سيفتح حينها صفحة إعدادات البطارية العامة بدل النافذة المباشرة.
  - إذن `SCHEDULE_EXACT_ALARM` يتطلب تعبئة نموذج «Exact alarm» في Play Console.
  - غيّر `appId` (حاليًا `com.adhkar.salawat`) قبل أول نشر إن أردت معرّفًا آخر، لأنه لا يتغير بعد النشر.

## المصادر
- نص المصحف وخطه: مجمع الملك فهد لطباعة المصحف الشريف (UthmanicHafs v3)، عبر حزمة [@quran.ws/text](https://github.com/quran-ws/quran-text) بترخيص CC BY 4.0.
