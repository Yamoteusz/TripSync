package pl.tripsync.data

import com.google.gson.JsonObject
import pl.tripsync.domain.User
import retrofit2.http.*

data class AuthRequest(val email: String, val password: String, val name: String = "")
data class AuthResponse(val token: String, val user: User)
data class RemoteRecord(val id: String, val tripId: String, val kind: String, val payload: JsonObject, val version: Int, val deleted: Boolean)
data class Snapshot(val tripIds: List<String>, val records: List<RemoteRecord>)
data class RemoteOperation(val id: String, val recordId: String, val tripId: String, val kind: String, val payload: JsonObject, val baseVersion: Int, val deleted: Boolean)
data class PushRequest(val operations: List<RemoteOperation>)
data class PushResult(val id: String, val status: String, val record: RemoteRecord? = null, val message: String? = null)
data class PushResponse(val results: List<PushResult>)
data class Invitation(val code: String, val tripId: String)
data class JoinRequest(val code: String)
data class JoinResponse(val tripId: String)

interface TripApi {
    @POST("auth/register") suspend fun register(@Body request: AuthRequest): AuthResponse
    @POST("auth/login") suspend fun login(@Body request: AuthRequest): AuthResponse
    @POST("auth/logout") suspend fun logout(@Header("Authorization") token: String): JsonObject
    @GET("sync") suspend fun snapshot(@Header("Authorization") token: String): Snapshot
    @POST("operations") suspend fun push(@Header("Authorization") token: String, @Body request: PushRequest): PushResponse
    @POST("join") suspend fun join(@Header("Authorization") token: String, @Body request: JoinRequest): JoinResponse
    @GET("trips/{id}/invite") suspend fun invite(@Header("Authorization") token: String, @Path("id") tripId: String): Invitation
    @POST("trips/{id}/invite") suspend fun rotateInvite(@Header("Authorization") token: String, @Path("id") tripId: String): Invitation
}
