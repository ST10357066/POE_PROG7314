package com.example.taskmaster.network

import com.example.taskmaster.data.Task
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// A simple wrapper for POST requests
data class CreateTaskRequest(
    val title: String,
    val description: String?,
    val dueDate: String? // Add the optional dueDate
)

data class FullUpdateTaskRequest(
    val title: String,
    val description: String?,
    val dueDate: String?
)

data class UpdateTaskRequest(val isDone: Boolean)
interface ApiService {

    @GET("/api/tasks")
    suspend fun getTasks(@Header("Authorization") token: String): List<Task>

    @POST("/api/tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Body task: CreateTaskRequest
    ): Task

    @PUT("/api/tasks/{id}")
    suspend fun updateTask(
        @Header("Authorization") token: String,
        @Path("id") taskId: String,
        @Body request: UpdateTaskRequest
    ): Task

    @PUT("/api/tasks/{id}")
    suspend fun updateTaskDetails(
        @Header("Authorization") token: String,
        @Path("id") taskId: String,
        @Body request: FullUpdateTaskRequest
    ): Response<Unit>

    @DELETE("/api/tasks/{id}")
    suspend fun deleteTask(
        @Header("Authorization") token: String,
        @Path("id") taskId: String
    ): Response<Unit>
}

object RetrofitInstance {
    // Replace with your Deno Deploy URL
    private const val BASE_URL = "https://tmr-api.vercel.app/"

    // --- 2. CREATE THE LOGGING INTERCEPTOR ---
    // This will log the full request URL, headers, and body
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Use .BODY to see everything
    }

    // --- 3. CREATE AN OkHttpClient AND ADD THE INTERCEPTOR ---
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}