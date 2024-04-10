package com.artf.chatapp.data.model

import com.google.gson.annotations.SerializedName

data class Evidence(
    var uri: String,
)

data class PostEvidenceRequest (
    var url: String
)

data class EvidenceResponse(
    @SerializedName("status_code")
    var status: Int,

    @SerializedName("message")
    var message: String,
)