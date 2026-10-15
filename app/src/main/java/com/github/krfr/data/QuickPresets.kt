package com.github.krfr.data

/** 快捷方案：常用「国家码 + 运营商」组合，一键应用 */
object QuickPresets {
    data class QuickPreset(
        val id: String,
        val name: String,
        val description: String,
        val countryCode: String,
        val carrierName: String
    )

    val presets = listOf(
        QuickPreset("hk_cmhk", "香港 CMHK", "中国移动香港 · 国家码 HK", "HK", "CMHK"),
        QuickPreset("hk_hkt", "香港 HKT", "香港电讯 · 国家码 HK", "HK", "HKT"),
        QuickPreset("hk_3hk", "香港 3HK", "3 香港 · 国家码 HK", "HK", "3HK"),
        QuickPreset("jp_docomo", "日本 docomo", "NTT docomo · 国家码 JP", "JP", "NTT docomo"),
        QuickPreset("jp_au", "日本 au", "au by KDDI · 国家码 JP", "JP", "au by KDDI"),
        QuickPreset("jp_softbank", "日本 SoftBank", "SoftBank · 国家码 JP", "JP", "Softbank"),
        QuickPreset("kr_sk", "韩国 SK Telecom", "SK Telecom · 国家码 KR", "KR", "SK Telecom"),
        QuickPreset("kr_kt", "韩国 KT", "KT Corporation · 国家码 KR", "KR", "KT Corporation"),
        QuickPreset("us_tmobile", "美国 T-Mobile", "T-Mobile USA · 国家码 US", "US", "T-Mobile USA"),
        QuickPreset("us_att", "美国 AT&T", "AT&T · 国家码 US", "US", "AT&T"),
        QuickPreset("us_verizon", "美国 Verizon", "Verizon · 国家码 US", "US", "Verizon"),
        QuickPreset("gb_ee", "英国 EE", "EE · 国家码 GB", "GB", "EE"),
        QuickPreset("gb_o2", "英国 O2", "O2 UK · 国家码 GB", "GB", "O2 UK"),
        QuickPreset("sg_singtel", "新加坡 Singtel", "Singtel · 国家码 SG", "SG", "Singtel"),
        QuickPreset("th_ais", "泰国 AIS", "AIS · 国家码 TH", "TH", "AIS"),
        QuickPreset("au_telstra", "澳大利亚 Telstra", "Telstra · 国家码 AU", "AU", "Telstra"),
        QuickPreset("de_tmobile", "德国 T-Mobile", "T-Mobile DE · 国家码 DE", "DE", "T-Mobile DE"),
        QuickPreset("fr_orange", "法国 Orange", "Orange FR · 国家码 FR", "FR", "Orange FR")
    )
}
