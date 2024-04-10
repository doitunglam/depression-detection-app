package com.artf.chatapp.data.model

import com.google.gson.annotations.SerializedName

data class Diagnose(
    var result: String? = null
)

data class DiagnoseResponse(
    @SerializedName("status_code")
    var status: Int,

    @SerializedName("message")
    var message: String,

    @SerializedName("diagnose")
    var diagnose: Diagnose
)