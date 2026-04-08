package com.example.features.setting.utils

import com.blankj.utilcode.util.LanguageUtils
import java.util.Locale

class LanguageController private constructor() {

    companion object {
        const val ENGLISH = "en"           // 英语
        const val SPANISH = "es"           // 西班牙语
        const val PORTUGUESE = "pt"        // 葡萄牙语
        const val KOREAN = "kr"            // 韩语
        const val JAPANESE = "jp"          // 日语
        const val FRENCH = "fr"            // 法语
        const val GERMAN = "de"            // 德语
        const val TURKISH = "tr"           // 土耳其语
        const val RUSSIAN = "ru"           // 俄语
        const val CHINESE_TW = "zh_tw"     // 繁体中文
        const val CHINESE_CN = "zh_cn"     // 简体中文
        const val THAI = "th"              // 泰语
        const val VIETNAMESE = "vn"        // 越南语
        const val ARABIC = "arb"           // 阿拉伯语
        const val INDONESIAN = "id"        // 印尼语
        const val ITALIAN = "it"           // 意大利语
        const val DANISH = "da"            // 丹麦语
        const val PERSIAN = "fa"           // 波斯语
        const val SWEDISH = "sv"           // 瑞典语

        private val languageSampleMap: Map<String, String> = mapOf(
            ENGLISH to "English",
            SPANISH to "Español",
            PORTUGUESE to "Português",
            KOREAN to "한국어",
            JAPANESE to "日本語",
            FRENCH to "Français",
            GERMAN to "Deutsch",
            TURKISH to "Türkçe",
            RUSSIAN to "Русский",
            CHINESE_TW to "中文繁體",
            CHINESE_CN to "中文简体",
            THAI to "ไทย",
            VIETNAMESE to "Tiếng Việt",
            ARABIC to "العربية",
            INDONESIAN to "Bahasa Indonesia",
            ITALIAN to "Italiano",
            DANISH to "Dansk",
            PERSIAN to "فارسی",
            SWEDISH to "Svenska"
        )


        private val localeMap: Map<String, Locale> = mapOf(
            ENGLISH to Locale("en"),                // 英语
            SPANISH to Locale("es"),                // 西班牙语
            PORTUGUESE to Locale("pt", "BR"),  // 葡萄牙语（巴西）
            KOREAN to Locale("ko", "KR"),  // 韩语（韩国）
            JAPANESE to Locale("ja", "JP"),  // 日语（日本）
            FRENCH to Locale("fr"),  // 法语
            GERMAN to Locale("de"),  // 德语
            TURKISH to Locale("tr"),  // 土耳其语
            RUSSIAN to Locale("ru"),  // 俄语
            CHINESE_TW to Locale("zh", "TW"),  // 台湾繁体中文
            CHINESE_CN to Locale("zh", "CN"),  // 简体中文
            THAI to Locale("th"),  // 泰语
            VIETNAMESE to Locale("vi"),  // 越南语
            ARABIC to Locale("ar"),  // 阿拉伯语
            INDONESIAN to Locale("id"),  // 印尼语
            ITALIAN to Locale("it"),  // 意大利语
            DANISH to Locale("da"),  // 丹麦语
            PERSIAN to Locale("fa"),  // 波斯语
            SWEDISH to Locale("sv"),  // 瑞典语
        )


        @Volatile
        private var INSTANCE: LanguageController? = null

        /**
         * 获取单例实例
         */
        fun getInstance(): LanguageController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanguageController().also { INSTANCE = it }
            }
        }
    }

    fun apply(aliens: String) {
        localeMap[aliens]?.let {
            LanguageUtils.applyLanguage(it)
        }
    }

    fun getAliens(): String {
        val locale = LanguageUtils.getAppliedLanguage() ?: LanguageUtils.getAppContextLanguage()
        val key = localeMap.entries.find { entry ->
            val mapLocale = entry.value
            // 匹配语言代码和国家代码
            mapLocale.language == locale.language &&
                    (mapLocale.country.isEmpty() || mapLocale.country == locale.country)
        }?.key
        return key ?: ENGLISH
    }

    fun sample(aliens: String = getAliens()): String {
        return languageSampleMap[aliens] ?: languageSampleMap.values.first()
    }

    /**
     * 获取所有支持的语言代码和显示名称
     */
    fun getAllLanguages(): Map<String, String> {
        return languageSampleMap
    }


}

