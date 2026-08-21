package com.example.ui.localization

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", true),
    ENGLISH("en", "English", false),
    RUSSIAN("ru", "Русский", false);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}

class AppStrings(val lang: AppLanguage) {
    // App Header & Branding
    val appTitle: String = when (lang) {
        AppLanguage.ARABIC -> "SWF-editor"
        AppLanguage.RUSSIAN -> "SWF-editor"
        AppLanguage.ENGLISH -> "SWF-editor"
    }

    val appTagline: String = when (lang) {
        AppLanguage.ARABIC -> "محرر نصوص وحوارات SWF خفيف ومتخصص"
        AppLanguage.RUSSIAN -> "Легкий специализированный редактор текста SWF"
        AppLanguage.ENGLISH -> "Lightweight specialized SWF text editor"
    }

    // Navigation Tabs
    val tabOverview: String = when (lang) {
        AppLanguage.ARABIC -> "نظرة عامة"
        AppLanguage.RUSSIAN -> "Обзор"
        AppLanguage.ENGLISH -> "Overview"
    }

    val tabTexts: String = when (lang) {
        AppLanguage.ARABIC -> "النصوص"
        AppLanguage.RUSSIAN -> "Тексты"
        AppLanguage.ENGLISH -> "Texts"
    }

    val tabTranslation: String = when (lang) {
        AppLanguage.ARABIC -> "وضع الترجمة"
        AppLanguage.RUSSIAN -> "Перевод"
        AppLanguage.ENGLISH -> "Translation"
    }

    val tabChanges: String = when (lang) {
        AppLanguage.ARABIC -> "التعديلات"
        AppLanguage.RUSSIAN -> "Изменения"
        AppLanguage.ENGLISH -> "Changes"
    }

    val tabBuild: String = when (lang) {
        AppLanguage.ARABIC -> "بناء وتصدير"
        AppLanguage.RUSSIAN -> "Сборка"
        AppLanguage.ENGLISH -> "Build"
    }

    val tabSettings: String = when (lang) {
        AppLanguage.ARABIC -> "الإعدادات"
        AppLanguage.RUSSIAN -> "Настройки"
        AppLanguage.ENGLISH -> "Settings"
    }

    val tabAbout: String = when (lang) {
        AppLanguage.ARABIC -> "حول"
        AppLanguage.RUSSIAN -> "О программе"
        AppLanguage.ENGLISH -> "About"
    }

    // Actions & Buttons
    val openSwf: String = when (lang) {
        AppLanguage.ARABIC -> "فتح ملف SWF"
        AppLanguage.RUSSIAN -> "Открыть SWF"
        AppLanguage.ENGLISH -> "Open SWF"
    }

    val openProject: String = when (lang) {
        AppLanguage.ARABIC -> "فتح مشروع"
        AppLanguage.RUSSIAN -> "Открыть проект"
        AppLanguage.ENGLISH -> "Open Project"
    }

    val saveProject: String = when (lang) {
        AppLanguage.ARABIC -> "حفظ المشروع"
        AppLanguage.RUSSIAN -> "Сохранить проект"
        AppLanguage.ENGLISH -> "Save Project"
    }

    val buildSwf: String = when (lang) {
        AppLanguage.ARABIC -> "بناء SWF الآن"
        AppLanguage.RUSSIAN -> "Собрать SWF"
        AppLanguage.ENGLISH -> "Build SWF"
    }

    val exportFile: String = when (lang) {
        AppLanguage.ARABIC -> "تصدير وحفظ في الهاتف"
        AppLanguage.RUSSIAN -> "Экспорт на устройство"
        AppLanguage.ENGLISH -> "Save to Device"
    }

    val shareFile: String = when (lang) {
        AppLanguage.ARABIC -> "مشاركة الملف"
        AppLanguage.RUSSIAN -> "Поделиться файлом"
        AppLanguage.ENGLISH -> "Share File"
    }

    val resetText: String = when (lang) {
        AppLanguage.ARABIC -> "استعادة الأصل"
        AppLanguage.RUSSIAN -> "Сбросить"
        AppLanguage.ENGLISH -> "Reset to Original"
    }

    val resetAllChanges: String = when (lang) {
        AppLanguage.ARABIC -> "إلغاء جميع التعديلات"
        AppLanguage.RUSSIAN -> "Отменить все изменения"
        AppLanguage.ENGLISH -> "Reset All Changes"
    }

    // Dashboard Cards
    val recentProjects: String = when (lang) {
        AppLanguage.ARABIC -> "المشاريع الأخيرة"
        AppLanguage.RUSSIAN -> "Недавние проекты"
        AppLanguage.ENGLISH -> "Recent Projects"
    }

    val totalTextObjects: String = when (lang) {
        AppLanguage.ARABIC -> "إجمالي كائنات النصوص"
        AppLanguage.RUSSIAN -> "Всего текстовых объектов"
        AppLanguage.ENGLISH -> "Total Text Objects"
    }

    val modifiedTexts: String = when (lang) {
        AppLanguage.ARABIC -> "نصوص معدلة"
        AppLanguage.RUSSIAN -> "Измененных текстов"
        AppLanguage.ENGLISH -> "Modified Texts"
    }

    val unchangedTexts: String = when (lang) {
        AppLanguage.ARABIC -> "نصوص غير معدلة"
        AppLanguage.RUSSIAN -> "Без изменений"
        AppLanguage.ENGLISH -> "Unchanged Texts"
    }

    val swfVersion: String = when (lang) {
        AppLanguage.ARABIC -> "إصدار SWF"
        AppLanguage.RUSSIAN -> "Версия SWF"
        AppLanguage.ENGLISH -> "SWF Version"
    }

    val compressionType: String = when (lang) {
        AppLanguage.ARABIC -> "نوع الضغط"
        AppLanguage.RUSSIAN -> "Сжатие"
        AppLanguage.ENGLISH -> "Compression"
    }

    val frameRate: String = when (lang) {
        AppLanguage.ARABIC -> "معدل الإطارات"
        AppLanguage.RUSSIAN -> "Частота кадров"
        AppLanguage.ENGLISH -> "Frame Rate"
    }

    val frameCount: String = when (lang) {
        AppLanguage.ARABIC -> "عدد الإطارات"
        AppLanguage.RUSSIAN -> "Всего кадров"
        AppLanguage.ENGLISH -> "Frame Count"
    }

    val dimensions: String = when (lang) {
        AppLanguage.ARABIC -> "الأبعاد"
        AppLanguage.RUSSIAN -> "Разрешение"
        AppLanguage.ENGLISH -> "Dimensions"
    }

    // Text Editor Labels
    val originalTextLabel: String = when (lang) {
        AppLanguage.ARABIC -> "النص الأصلي (للقراءة فقط)"
        AppLanguage.RUSSIAN -> "Оригинальный текст (только чтение)"
        AppLanguage.ENGLISH -> "Original Text (Read-only)"
    }

    val editedTextLabel: String = when (lang) {
        AppLanguage.ARABIC -> "النص بعد التعديل"
        AppLanguage.RUSSIAN -> "Отредактированный текст"
        AppLanguage.ENGLISH -> "Edited Text"
    }

    val characterCount: String = when (lang) {
        AppLanguage.ARABIC -> "عدد الأحرف"
        AppLanguage.RUSSIAN -> "Количество символов"
        AppLanguage.ENGLISH -> "Characters"
    }

    val fontNameLabel: String = when (lang) {
        AppLanguage.ARABIC -> "الخط المستخدم"
        AppLanguage.RUSSIAN -> "Шрифт"
        AppLanguage.ENGLISH -> "Font"
    }

    val frameLabel: String = when (lang) {
        AppLanguage.ARABIC -> "الإطار"
        AppLanguage.RUSSIAN -> "Кадр"
        AppLanguage.ENGLISH -> "Frame"
    }

    val typeLabel: String = when (lang) {
        AppLanguage.ARABIC -> "النوع"
        AppLanguage.RUSSIAN -> "Тип"
        AppLanguage.ENGLISH -> "Type"
    }

    // Search & Filters
    val searchPlaceholder: String = when (lang) {
        AppLanguage.ARABIC -> "بحث في النصوص والحوارات..."
        AppLanguage.RUSSIAN -> "Поиск по текстам..."
        AppLanguage.ENGLISH -> "Search texts and dialogues..."
    }

    val replacePlaceholder: String = when (lang) {
        AppLanguage.ARABIC -> "استبدال بـ..."
        AppLanguage.RUSSIAN -> "Заменить на..."
        AppLanguage.ENGLISH -> "Replace with..."
    }

    val replaceButton: String = when (lang) {
        AppLanguage.ARABIC -> "استبدال"
        AppLanguage.RUSSIAN -> "Заменить"
        AppLanguage.ENGLISH -> "Replace"
    }

    val replaceAllButton: String = when (lang) {
        AppLanguage.ARABIC -> "استبدال الكل"
        AppLanguage.RUSSIAN -> "Заменить все"
        AppLanguage.ENGLISH -> "Replace All"
    }

    val filterAll: String = when (lang) {
        AppLanguage.ARABIC -> "الكل"
        AppLanguage.RUSSIAN -> "Все"
        AppLanguage.ENGLISH -> "All"
    }

    val filterModified: String = when (lang) {
        AppLanguage.ARABIC -> "المعدلة فقط"
        AppLanguage.RUSSIAN -> "Измененные"
        AppLanguage.ENGLISH -> "Modified"
    }

    val filterUnmodified: String = when (lang) {
        AppLanguage.ARABIC -> "غير المعدلة"
        AppLanguage.RUSSIAN -> "Без изменений"
        AppLanguage.ENGLISH -> "Unmodified"
    }

    val filterEmpty: String = when (lang) {
        AppLanguage.ARABIC -> "فارغة"
        AppLanguage.RUSSIAN -> "Пустые"
        AppLanguage.ENGLISH -> "Empty"
    }

    // Warnings & Notices
    val fontCompatibilityWarning: String = when (lang) {
        AppLanguage.ARABIC -> "تنبيه: الخط المضمن في SWF قد لا يحتوي على جميع المحارف المطلوبة، وقد تظهر كفراغات داخل المشغل الأصلي."
        AppLanguage.RUSSIAN -> "Предупреждение: Встроенный шрифт SWF может не содержать всех символов кириллицы/арабского."
        AppLanguage.ENGLISH -> "Warning: The embedded SWF font may not contain all target glyphs. Text might render with fallback in target player."
    }

    val noFileLoaded: String = when (lang) {
        AppLanguage.ARABIC -> "لم يتم فتح أي ملف SWF حالياً"
        AppLanguage.RUSSIAN -> "SWF файл не загружен"
        AppLanguage.ENGLISH -> "No SWF file loaded"
    }

    val noFileLoadedDesc: String = when (lang) {
        AppLanguage.ARABIC -> "اضغط على زر 'فتح ملف SWF' لتحليل النصوص وبدء التحرير والترجمة."
        AppLanguage.RUSSIAN -> "Нажмите «Открыть SWF» для анализа и редактирования текстов."
        AppLanguage.ENGLISH -> "Tap 'Open SWF' to extract texts and start editing or translating."
    }

    val readyToBuild: String = when (lang) {
        AppLanguage.ARABIC -> "جاهز للبناء والتصدير"
        AppLanguage.RUSSIAN -> "Готово к сборке"
        AppLanguage.ENGLISH -> "Ready to build"
    }

    val buildSuccess: String = when (lang) {
        AppLanguage.ARABIC -> "تم بناء ملف SWF بنجاح!"
        AppLanguage.RUSSIAN -> "SWF успешно собран!"
        AppLanguage.ENGLISH -> "SWF built successfully!"
    }

    val buildFailed: String = when (lang) {
        AppLanguage.ARABIC -> "فشل البناء: لم يتم المساس بالملف الأصلي."
        AppLanguage.RUSSIAN -> "Ошибка сборки: оригинальный файл не изменен."
        AppLanguage.ENGLISH -> "Build failed: original file has not been modified."
    }

    // Translation Tools
    val importTranslations: String = when (lang) {
        AppLanguage.ARABIC -> "استيراد ترجمات (JSON / CSV)"
        AppLanguage.RUSSIAN -> "Импорт перевода (JSON / CSV)"
        AppLanguage.ENGLISH -> "Import Translations (JSON / CSV)"
    }

    val exportTranslations: String = when (lang) {
        AppLanguage.ARABIC -> "تصدير نصوص للترجمة"
        AppLanguage.RUSSIAN -> "Экспорт текстов"
        AppLanguage.ENGLISH -> "Export Translations"
    }

    // About Section
    val aboutDeveloper: String = when (lang) {
        AppLanguage.ARABIC -> "المطور: CLXV11"
        AppLanguage.RUSSIAN -> "Разработчик: CLXV11"
        AppLanguage.ENGLISH -> "Developer: CLXV11"
    }

    val aboutDescription: String = when (lang) {
        AppLanguage.ARABIC -> "SWF-editor هو تطبيق خفيف متخصص في تعديل نصوص وحوارات ملفات فلاش (SWF) مع الحفاظ التام على الصور والأصوات والرسومات."
        AppLanguage.RUSSIAN -> "SWF-editor — легкий инструмент для изменения текста и диалогов в SWF файлах с сохранением всех ресурсов."
        AppLanguage.ENGLISH -> "SWF-editor is a lightweight, specialized tool to edit Flash (SWF) text and dialogues while preserving all multimedia resources."
    }

    val openGithub: String = when (lang) {
        AppLanguage.ARABIC -> "زيارة مستودع GitHub"
        AppLanguage.RUSSIAN -> "Открыть GitHub"
        AppLanguage.ENGLISH -> "Visit GitHub"
    }

    val selectLanguage: String = when (lang) {
        AppLanguage.ARABIC -> "لغة التطبيق"
        AppLanguage.RUSSIAN -> "Язык интерфейса"
        AppLanguage.ENGLISH -> "App Language"
    }

    val themeMode: String = when (lang) {
        AppLanguage.ARABIC -> "المظهر"
        AppLanguage.RUSSIAN -> "Тема"
        AppLanguage.ENGLISH -> "Theme"
    }

    val darkTheme: String = when (lang) {
        AppLanguage.ARABIC -> "الوضع الداكن"
        AppLanguage.RUSSIAN -> "Темная тема"
        AppLanguage.ENGLISH -> "Dark Theme"
    }

    val lightTheme: String = when (lang) {
        AppLanguage.ARABIC -> "الوضع الفاتح"
        AppLanguage.RUSSIAN -> "Светлая тема"
        AppLanguage.ENGLISH -> "Light Theme"
    }

    val autoSave: String = when (lang) {
        AppLanguage.ARABIC -> "الحفظ التلقائي (كل 5 ثوانٍ)"
        AppLanguage.RUSSIAN -> "Автосохранение (каждые 5 сек)"
        AppLanguage.ENGLISH -> "Autosave (every 5 seconds)"
    }
}
