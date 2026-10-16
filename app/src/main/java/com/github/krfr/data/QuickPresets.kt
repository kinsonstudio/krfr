package com.github.krfr.data

/** 快捷方案：常用「国家码 + 运营商」组合，一键应用 */
object QuickPresets {
    data class QuickPreset(
        val id: String,
        /** 简体中文方案名（数据默认） */
        val name: String,
        /** 英文方案名 */
        val enName: String,
        /** 繁体中文方案名 */
        val twName: String,
        /** 简体中文描述（数据默认） */
        val description: String,
        /** 英文描述 */
        val enDesc: String,
        /** 繁体中文描述 */
        val twDesc: String,
        val countryCode: String,
        val carrierName: String
    )

    val presets = listOf(
        QuickPreset("hk_cmhk", "香港 CMHK", "HK CMHK", "香港 CMHK", "中国移动香港 · 国家码 HK", "China Mobile HK · Country code HK", "中國移動香港 · 國家碼 HK", "HK", "CMHK"),
        QuickPreset("hk_hkt", "香港 HKT", "HK HKT", "香港 HKT", "香港电讯 · 国家码 HK", "HKT · Country code HK", "香港電訊 · 國家碼 HK", "HK", "HKT"),
        QuickPreset("hk_3hk", "香港 3HK", "HK 3HK", "香港 3HK", "3 香港 · 国家码 HK", "3 Hong Kong · Country code HK", "3 香港 · 國家碼 HK", "HK", "3HK"),
        QuickPreset("jp_docomo", "日本 docomo", "Japan docomo", "日本 docomo", "NTT docomo · 国家码 JP", "NTT docomo · Country code JP", "NTT docomo · 國家碼 JP", "JP", "NTT docomo"),
        QuickPreset("jp_au", "日本 au", "Japan au", "日本 au", "au by KDDI · 国家码 JP", "au by KDDI · Country code JP", "au by KDDI · 國家碼 JP", "JP", "au by KDDI"),
        QuickPreset("jp_softbank", "日本 SoftBank", "Japan SoftBank", "日本 SoftBank", "SoftBank · 国家码 JP", "SoftBank · Country code JP", "SoftBank · 國家碼 JP", "JP", "Softbank"),
        QuickPreset("kr_sk", "韩国 SK Telecom", "South Korea SK Telecom", "韓國 SK Telecom", "SK Telecom · 国家码 KR", "SK Telecom · Country code KR", "SK Telecom · 國家碼 KR", "KR", "SK Telecom"),
        QuickPreset("kr_kt", "韩国 KT", "South Korea KT", "韓國 KT", "KT Corporation · 国家码 KR", "KT Corporation · Country code KR", "KT Corporation · 國家碼 KR", "KR", "KT Corporation"),
        QuickPreset("us_tmobile", "美国 T-Mobile", "US T-Mobile", "美國 T-Mobile", "T-Mobile USA · 国家码 US", "T-Mobile USA · Country code US", "T-Mobile USA · 國家碼 US", "US", "T-Mobile USA"),
        QuickPreset("us_att", "美国 AT&T", "US AT&T", "美國 AT&T", "AT&T · 国家码 US", "AT&T · Country code US", "AT&T · 國家碼 US", "US", "AT&T"),
        QuickPreset("us_verizon", "美国 Verizon", "US Verizon", "美國 Verizon", "Verizon · 国家码 US", "Verizon · Country code US", "Verizon · 國家碼 US", "US", "Verizon"),
        QuickPreset("gb_ee", "英国 EE", "UK EE", "英國 EE", "EE · 国家码 GB", "EE · Country code GB", "EE · 國家碼 GB", "GB", "EE"),
        QuickPreset("gb_o2", "英国 O2", "UK O2", "英國 O2", "O2 UK · 国家码 GB", "O2 UK · Country code GB", "O2 UK · 國家碼 GB", "GB", "O2 UK"),
        QuickPreset("sg_singtel", "新加坡 Singtel", "Singapore Singtel", "新加坡 Singtel", "Singtel · 国家码 SG", "Singtel · Country code SG", "Singtel · 國家碼 SG", "SG", "Singtel"),
        QuickPreset("th_ais", "泰国 AIS", "Thailand AIS", "泰國 AIS", "AIS · 国家码 TH", "AIS · Country code TH", "AIS · 國家碼 TH", "TH", "AIS"),
        QuickPreset("au_telstra", "澳大利亚 Telstra", "Australia Telstra", "澳洲 Telstra", "Telstra · 国家码 AU", "Telstra · Country code AU", "Telstra · 國家碼 AU", "AU", "Telstra"),
        QuickPreset("de_tmobile", "德国 T-Mobile", "Germany T-Mobile", "德國 T-Mobile", "T-Mobile DE · 国家码 DE", "T-Mobile DE · Country code DE", "T-Mobile DE · 國家碼 DE", "DE", "T-Mobile DE"),
        QuickPreset("fr_orange", "法国 Orange", "France Orange", "法國 Orange", "Orange FR · 国家码 FR", "Orange FR · Country code FR", "Orange FR · 國家碼 FR", "FR", "Orange FR")
    )
}

/** 按当前应用语言取方案名：en / zh-CN / zh-TW */
fun QuickPresets.QuickPreset.nameFor(lang: String?): String = when (lang) {
    "zh-CN" -> name
    "zh-TW" -> twName
    else -> enName
}

/** 按当前应用语言取方案描述：en / zh-CN / zh-TW */
fun QuickPresets.QuickPreset.descFor(lang: String?): String = when (lang) {
    "zh-CN" -> description
    "zh-TW" -> twDesc
    else -> enDesc
}
