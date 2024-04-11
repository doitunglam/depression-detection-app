package com.artf.chatapp.api

import com.artf.chatapp.data.model.DiagnoseResponse
import com.artf.chatapp.data.model.EvidenceResponse
import com.artf.chatapp.data.model.PostEvidenceRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("diagnose")
    fun getDiagnose(): Call<DiagnoseResponse>;

    @POST("evidence")
    fun postEvidence(@Body body: PostEvidenceRequest): Call<EvidenceResponse>;
}
