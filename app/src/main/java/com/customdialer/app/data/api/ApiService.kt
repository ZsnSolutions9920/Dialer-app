package com.customdialer.app.data.api

import com.customdialer.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth (Agent — legacy)
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    // Customer Auth (SaaS)
    @POST("api/customer/signup")
    suspend fun customerSignup(@Body request: CustomerSignupRequest): Response<CustomerAuthResponse>

    @POST("api/customer/login")
    suspend fun customerLogin(@Body request: CustomerLoginRequest): Response<CustomerAuthResponse>

    @POST("api/customer/refresh")
    suspend fun customerRefresh(@Body request: RefreshRequest): Response<CustomerAuthResponse>

    @GET("api/customer/me")
    suspend fun getCustomerProfile(): Response<CustomerProfile>

    // Agents
    @GET("api/agents")
    suspend fun getAgents(): Response<List<Agent>>

    @GET("api/agents/me")
    suspend fun getMyProfile(): Response<Agent>

    @PATCH("api/agents/{id}/status")
    suspend fun updateAgentStatus(
        @Path("id") id: Int,
        @Body status: AgentStatusUpdate
    ): Response<Agent>

    // Call Logs
    @GET("api/calls")
    suspend fun getCallLogs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("direction") direction: String? = null
    ): Response<CallLogsResponse>

    @GET("api/calls/stats")
    suspend fun getCallStats(): Response<DashboardStats>

    @GET("api/calls/stats/today-count")
    suspend fun getTodayCallCount(): Response<TodayCount>

    @GET("api/calls/stats/agent-leaderboard")
    suspend fun getLeaderboard(): Response<List<LeaderboardEntry>>

    @GET("api/calls/active")
    suspend fun getActiveCalls(): Response<List<ActiveCall>>

    @PATCH("api/calls/{id}/notes")
    suspend fun updateCallNotes(
        @Path("id") id: Int,
        @Body notes: CallNotesUpdate
    ): Response<CallLog>

    @DELETE("api/calls/{id}")
    suspend fun deleteCallLog(@Path("id") id: Int): Response<Unit>

    // Contacts
    @GET("api/contacts")
    suspend fun getContacts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<ContactsResponse>

    @GET("api/contacts/{id}")
    suspend fun getContact(@Path("id") id: Int): Response<Contact>

    @GET("api/contacts/lookup/{phoneNumber}")
    suspend fun lookupContact(@Path("phoneNumber") phoneNumber: String): Response<Contact>

    @POST("api/contacts")
    suspend fun createContact(@Body contact: ContactCreate): Response<Contact>

    @PUT("api/contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: Int,
        @Body contact: ContactCreate
    ): Response<Contact>

    @PATCH("api/contacts/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: Int): Response<Contact>

    @DELETE("api/contacts/{id}")
    suspend fun deleteContact(@Path("id") id: Int): Response<Unit>

    // Phone Lists
    @GET("api/phone-lists")
    suspend fun getPhoneLists(): Response<List<PhoneList>>

    @GET("api/phone-lists/{id}/entries")
    suspend fun getPhoneListEntries(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<PhoneListEntriesResponse>

    @GET("api/phone-lists/{id}/power-dial-progress")
    suspend fun getPowerDialProgress(@Path("id") id: Int): Response<PowerDialProgress>

    @GET("api/phone-lists/{id}/next-dialable")
    suspend fun getNextDialableEntry(@Path("id") id: Int): Response<PhoneListEntry>

    @PATCH("api/phone-lists/entries/{entryId}/status")
    suspend fun updateEntryStatus(
        @Path("entryId") entryId: Int,
        @Body status: Map<String, String>
    ): Response<PhoneListEntry>

    @DELETE("api/phone-lists/{id}")
    suspend fun deletePhoneList(@Path("id") id: Int): Response<Unit>

    // Email
    @GET("api/email/smtp")
    suspend fun getSmtpConfigs(): Response<List<SmtpConfig>>

    @GET("api/email/templates")
    suspend fun getEmailTemplates(): Response<List<EmailTemplate>>

    @GET("api/email/inbox")
    suspend fun getInbox(
        @Query("folder") folder: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<EmailsResponse>

    @GET("api/email/inbox/unread-count")
    suspend fun getUnreadCount(): Response<Map<String, Int>>

    @GET("api/email/inbox/{id}")
    suspend fun getEmail(@Path("id") id: Int): Response<Email>

    // Store
    @GET("api/store/numbers")
    suspend fun getAvailableNumbers(
        @Query("country") country: String = "US",
        @Query("type") type: String = "local",
        @Query("limit") limit: Int = 10
    ): Response<AvailableNumbersResponse>

    @GET("api/store/minutes-packages")
    suspend fun getMinutesPackages(): Response<MinutesPackagesResponse>

    // Select Number
    @POST("api/purchase/select-number")
    suspend fun selectNumber(@Body request: SelectNumberRequest): Response<SelectNumberResponse>

    // Payment Method
    @GET("api/purchase/payment-method")
    suspend fun getPaymentMethod(): Response<PaymentMethodInfo>

    @POST("api/purchase/setup-card")
    suspend fun setupCard(): Response<SetupCardResponse>

    @POST("api/purchase/charge")
    suspend fun chargeCard(@Body request: ChargeRequest): Response<MockPurchaseResponse>

    // Customer call history
    @GET("api/purchase/my-calls")
    suspend fun getMyCallHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<CallLogsResponse>

    // Calling Status & Token
    @GET("api/purchase/calling-status")
    suspend fun getCallingStatus(): Response<CallingStatus>

    @GET("api/purchase/twilio-token")
    suspend fun getCustomerTwilioToken(): Response<CustomerTwilioToken>

    // Purchases
    @POST("api/purchase/mock")
    suspend fun mockPurchase(@Body request: MockPurchaseRequest): Response<MockPurchaseResponse>

    @GET("api/purchase/history")
    suspend fun getPurchaseHistory(): Response<PurchaseHistoryResponse>

    @GET("api/purchase/my-minutes")
    suspend fun getMyMinutes(): Response<MinutesSummary>

    @GET("api/purchase/my-numbers")
    suspend fun getMyNumbers(): Response<MyNumbersResponse>

    // Twilio Token
    @GET("api/token")
    suspend fun getTwilioToken(): Response<TwilioTokenResponse>

    // Call actions
    @POST("api/calls/hangup")
    suspend fun hangupCall(@Body body: Map<String, String>): Response<Unit>

    @POST("api/twilio/accept-mobile")
    suspend fun acceptInboundMobile(@Body body: Map<String, String>): Response<Map<String, String>>

    // Attendance
    @POST("api/attendance/clock-in")
    suspend fun clockIn(): Response<AttendanceLog>

    @POST("api/attendance/clock-out")
    suspend fun clockOut(): Response<AttendanceLog>

    @GET("api/attendance/current")
    suspend fun getCurrentAttendance(): Response<AttendanceSession>

    @GET("api/attendance/history/{agentId}")
    suspend fun getAttendanceHistory(
        @Path("agentId") agentId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<AttendanceLog>>
}
