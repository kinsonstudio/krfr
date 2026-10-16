package com.github.krfr.data

object CountryPresets {
    data class CountryInfo(
        val code: String,
        /** 简体中文名（数据默认） */
        val name: String,
        /** 英文名 */
        val en: String,
        /** 繁体中文名 */
        val tw: String
    )

    private val PRIORITY_CODES = listOf("CN", "HK", "MO", "TW")

    private val ALL_COUNTRIES = listOf(
        CountryInfo("CN", "中国", "China", "中國"),
        CountryInfo("HK", "中国香港", "Hong Kong, China", "中國香港"),
        CountryInfo("MO", "中国澳门", "Macao, China", "中國澳門"),
        CountryInfo("TW", "中国台湾", "Taiwan, China", "中國臺灣"),
        CountryInfo("JP", "日本", "Japan", "日本"),
        CountryInfo("KR", "韩国", "South Korea", "韓國"),
        CountryInfo("US", "美国", "United States", "美國"),
        CountryInfo("GB", "英国", "United Kingdom", "英國"),
        CountryInfo("DE", "德国", "Germany", "德國"),
        CountryInfo("FR", "法国", "France", "法國"),
        CountryInfo("IT", "意大利", "Italy", "意大利"),
        CountryInfo("ES", "西班牙", "Spain", "西班牙"),
        CountryInfo("PT", "葡萄牙", "Portugal", "葡萄牙"),
        CountryInfo("RU", "俄罗斯", "Russia", "俄羅斯"),
        CountryInfo("IN", "印度", "India", "印度"),
        CountryInfo("AU", "澳大利亚", "Australia", "澳洲"),
        CountryInfo("NZ", "新西兰", "New Zealand", "紐西蘭"),
        CountryInfo("SG", "新加坡", "Singapore", "新加坡"),
        CountryInfo("MY", "马来西亚", "Malaysia", "馬來西亞"),
        CountryInfo("TH", "泰国", "Thailand", "泰國"),
        CountryInfo("VN", "越南", "Vietnam", "越南"),
        CountryInfo("ID", "印度尼西亚", "Indonesia", "印尼"),
        CountryInfo("PH", "菲律宾", "Philippines", "菲律賓"),
        CountryInfo("CA", "加拿大", "Canada", "加拿大"),
        CountryInfo("MX", "墨西哥", "Mexico", "墨西哥"),
        CountryInfo("BR", "巴西", "Brazil", "巴西"),
        CountryInfo("AR", "阿根廷", "Argentina", "阿根廷"),
        CountryInfo("ZA", "南非", "South Africa", "南非")
    )

    /** 大中华区固定顺序，其余按简体名排序；运营商列表与此顺序一致。 */
    val countries: List<CountryInfo> = buildList {
        val byCode = ALL_COUNTRIES.associateBy { it.code }
        PRIORITY_CODES.mapNotNull { byCode[it] }.forEach { add(it) }
        ALL_COUNTRIES
            .filter { it.code !in PRIORITY_CODES }
            .sortedBy { it.name }
            .forEach { add(it) }
    }

    val countryOrder: Map<String, Int> = countries.withIndex().associate { (index, country) ->
        country.code to index
    }
}

/** 按当前应用语言取国家显示名：en / zh-CN / zh-TW */
fun CountryPresets.CountryInfo.nameFor(lang: String?): String = when (lang) {
    "zh-CN" -> name
    "zh-TW" -> tw
    else -> en
}
