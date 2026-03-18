package com.customdialer.app.data.model

import com.google.gson.annotations.SerializedName

// Auth (Agent — legacy)
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val agent: AgentPayload
)

data class AgentPayload(
    val id: Int,
    val username: String,
    val displayName: String?,
    val twilioIdentity: String?,
    val twilioPhoneNumber: String?
)

data class RefreshRequest(
    val refreshToken: String
)

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String
)

// Customer Auth (SaaS)
data class CustomerSignupRequest(
    val email: String,
    val password: String,
    val name: String?
)

data class CustomerLoginRequest(
    val email: String,
    val password: String
)

data class CustomerAuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val customer: CustomerPayload
)

data class CustomerPayload(
    val id: Int,
    val email: String,
    val name: String?,
    val role: String?
)

data class CustomerProfile(
    val id: Int,
    val email: String,
    val name: String?,
    val status: String?,
    @SerializedName("package") val pkg: String?,
    val createdAt: String?
)

// Twilio
data class TwilioTokenResponse(
    val token: String,
    val identity: String
)

// Agent
data class Agent(
    val id: Int,
    val username: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("twilio_identity") val twilioIdentity: String?,
    @SerializedName("twilio_phone_number") val twilioPhoneNumber: String?,
    val status: String?,
    val timezone: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class AgentStatusUpdate(
    val status: String
)

// Call Logs
data class CallLog(
    val id: Int,
    @SerializedName("call_sid") val callSid: String?,
    @SerializedName("agent_id") val agentId: Int?,
    @SerializedName("agent_name") val agentName: String?,
    val direction: String?,
    @SerializedName("from_number") val fromNumber: String?,
    @SerializedName("to_number") val toNumber: String?,
    val status: String?,
    val duration: Int?,
    val notes: String?,
    val disposition: String?,
    @SerializedName("recording_url") val recordingUrl: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class CallLogsResponse(
    val calls: List<CallLog>,
    val total: Int,
    val page: Int,
    val limit: Int
)

data class CallNotesUpdate(
    val notes: String?,
    val disposition: String?
)

// Dashboard Stats
data class DashboardStats(
    @SerializedName("total_calls") val totalCalls: Int?,
    @SerializedName("answered_calls") val answeredCalls: Int?,
    @SerializedName("missed_calls") val missedCalls: Int?,
    @SerializedName("avg_duration") val avgDuration: Double?,
    @SerializedName("total_duration") val totalDuration: Int?,
    @SerializedName("outbound_calls") val outboundCalls: Int?,
    @SerializedName("inbound_calls") val inboundCalls: Int?
)

data class TodayCount(
    val count: Int
)

data class LeaderboardEntry(
    @SerializedName("agent_id") val agentId: Int,
    @SerializedName("display_name") val displayName: String?,
    val username: String?,
    @SerializedName("call_count") val callCount: Int,
    @SerializedName("total_duration") val totalDuration: Int?
)

// Contacts
data class Contact(
    val id: Int,
    @SerializedName("agent_id") val agentId: Int?,
    val name: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    val email: String?,
    val company: String?,
    val notes: String?,
    @SerializedName("is_favorite") val isFavorite: Boolean?,
    @SerializedName("created_at") val createdAt: String?
)

data class ContactsResponse(
    val contacts: List<Contact>,
    val total: Int,
    val page: Int,
    val limit: Int
)

data class ContactCreate(
    val name: String,
    @SerializedName("phone_number") val phoneNumber: String,
    val email: String? = null,
    val company: String? = null,
    val notes: String? = null
)

// Phone Lists
data class PhoneList(
    val id: Int,
    @SerializedName("agent_id") val agentId: Int?,
    val name: String?,
    val description: String?,
    @SerializedName("total_entries") val totalEntries: Int?,
    @SerializedName("created_at") val createdAt: String?
)

data class PhoneListEntry(
    val id: Int,
    @SerializedName("phone_list_id") val phoneListId: Int?,
    @SerializedName("phone_number") val phoneNumber: String?,
    val name: String?,
    val email: String?,
    val status: String?,
    @SerializedName("follow_up_date") val followUpDate: String?,
    @SerializedName("lead_metadata") val leadMetadata: Any?,
    @SerializedName("created_at") val createdAt: String?
)

data class PhoneListEntriesResponse(
    val entries: List<PhoneListEntry>,
    val total: Int,
    val page: Int,
    val limit: Int
)

data class PowerDialProgress(
    val total: Int,
    val called: Int,
    val remaining: Int,
    val percentage: Double
)

// Email
data class SmtpConfig(
    val id: Int,
    @SerializedName("agent_id") val agentId: Int?,
    @SerializedName("smtp_host") val smtpHost: String?,
    @SerializedName("smtp_port") val smtpPort: Int?,
    @SerializedName("smtp_user") val smtpUser: String?,
    @SerializedName("from_email") val fromEmail: String?,
    @SerializedName("from_name") val fromName: String?,
    @SerializedName("imap_host") val imapHost: String?,
    @SerializedName("imap_port") val imapPort: Int?
)

data class EmailTemplate(
    val id: Int,
    @SerializedName("agent_id") val agentId: Int?,
    val name: String?,
    val subject: String?,
    val body: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class Email(
    val id: Int,
    @SerializedName("agent_id") val agentId: Int?,
    @SerializedName("message_id") val messageId: String?,
    @SerializedName("from_email") val fromEmail: String?,
    @SerializedName("to_email") val toEmail: String?,
    val subject: String?,
    val body: String?,
    val folder: String?,
    @SerializedName("is_read") val isRead: Boolean?,
    @SerializedName("created_at") val createdAt: String?
)

data class EmailsResponse(
    val emails: List<Email>,
    val total: Int
)

// Attendance
data class AttendanceLog(
    val id: Int,
    @SerializedName("agent_id") val agentId: Int?,
    @SerializedName("clock_in") val clockIn: String?,
    @SerializedName("clock_out") val clockOut: String?,
    val duration: Int?
)

data class AttendanceSession(
    val id: Int?,
    @SerializedName("clock_in") val clockIn: String?,
    @SerializedName("is_clocked_in") val isClockedIn: Boolean
)

// Store — Available Numbers
data class NumberCapabilities(
    val voice: Boolean?,
    val sms: Boolean?,
    val mms: Boolean?
)

data class AvailableNumber(
    val phoneNumber: String?,
    val friendlyName: String?,
    val locality: String?,
    val region: String?,
    val capabilities: NumberCapabilities?,
    val type: String?,
    val monthlyPrice: Double?
)

data class AvailableNumbersResponse(
    val numbers: List<AvailableNumber>,
    val type: String?,
    val country: String?
)

// Store — Minutes Packages
data class MinutesPackage(
    val id: String,
    val minutes: Int,
    val price: Double,
    val perMinute: Double,
    val savings: Int
)

data class MinutesPackagesResponse(
    val packages: List<MinutesPackage>
)

// Select Number
data class SelectNumberRequest(
    val phoneNumber: String
)

data class SelectNumberResponse(
    val selected: String?
)

// Payment Method
data class PaymentMethodInfo(
    val hasCard: Boolean?,
    val last4: String?,
    val brand: String?
)

data class SetupCardResponse(
    val url: String?
)

data class ChargeRequest(
    val type: String,
    val itemId: String?,
    val itemLabel: String?,
    val amount: Double?,
    val metadata: Map<String, Any?>?
)

// Calling Status
data class CallingStatus(
    val hasNumber: Boolean?,
    val phoneNumber: String?,
    val twilioIdentity: String?,
    val minutesTotal: Int?,
    val minutesUsed: Int?,
    val minutesRemaining: Int?,
    val canMakeCalls: Boolean?
)

data class CustomerTwilioToken(
    val token: String,
    val identity: String,
    val phoneNumber: String?
)

// Purchases
data class MockPurchaseRequest(
    val type: String,
    val itemId: String?,
    val itemLabel: String?,
    val amount: Double?,
    val metadata: Map<String, Any?>?
)

data class MockPurchaseResponse(
    val purchase: Purchase?,
    val message: String?
)

data class Purchase(
    val id: Int?,
    val type: String?,
    @SerializedName("item_label") val itemLabel: String?,
    val amount: Double?,
    val status: String?,
    val mock: Boolean?,
    @SerializedName("created_at") val createdAt: String?
)

data class PurchaseHistoryResponse(
    val purchases: List<Purchase>
)

data class MinutesSummary(
    @SerializedName("total_minutes") val totalMinutes: Int?,
    @SerializedName("used_minutes") val usedMinutes: Int?,
    @SerializedName("remaining_minutes") val remainingMinutes: Int?
)

data class MyNumbersResponse(
    val numbers: List<MyNumber>
)

data class MyNumber(
    val id: Int?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("friendly_name") val friendlyName: String?,
    val type: String?,
    @SerializedName("monthly_price") val monthlyPrice: Double?,
    val status: String?,
    @SerializedName("assigned_at") val assignedAt: String?
)

// Active Calls
data class ActiveCall(
    val id: Int?,
    @SerializedName("call_sid") val callSid: String?,
    @SerializedName("conference_sid") val conferenceSid: String?,
    @SerializedName("agent_id") val agentId: Int?,
    @SerializedName("agent_name") val agentName: String?,
    val direction: String?,
    @SerializedName("from_number") val fromNumber: String?,
    @SerializedName("to_number") val toNumber: String?,
    @SerializedName("created_at") val createdAt: String?
)
