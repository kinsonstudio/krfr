package com.github.krfr.model

/** 一张 SIM 卡的详细实时状态（用于状态面板展示） */
data class SimStatusInfo(
    val slot: Int,
    val subId: Int,
    val carrierName: String,
    val mcc: String,
    val mnc: String,
    val countryIso: String,
    val networkType: String,
    val signalLevel: Int,
    val signalDbm: Int,
    val dataRoaming: Boolean,
    val iccidMasked: String,
    val simState: String,
    val hasOverride: Boolean
)
