package com.customdialer.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.customdialer.app.data.api.RetrofitClient
import com.customdialer.app.data.api.SocketManager
import com.customdialer.app.data.api.TwilioVoiceManager
import com.customdialer.app.data.model.*
import com.customdialer.app.ui.theme.ThemeState
import com.customdialer.app.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val tokenManager = TokenManager(application)
    val twilioVoice = TwilioVoiceManager(application)
    private val socketManager = SocketManager()

    // Auth state
    private val _isLoggedIn = MutableStateFlow(tokenManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    // Agent
    private val _agent = MutableStateFlow<Agent?>(null)
    val agent: StateFlow<Agent?> = _agent

    // Customer recent calls (for dashboard)
    private val _customerCalls = MutableStateFlow<List<CallLog>>(emptyList())
    val customerCalls: StateFlow<List<CallLog>> = _customerCalls

    // Call state
    private val _isOnCall = MutableStateFlow(false)
    val isOnCall: StateFlow<Boolean> = _isOnCall

    private val _activeCallNumber = MutableStateFlow<String?>(null)
    val activeCallNumber: StateFlow<String?> = _activeCallNumber

    private val _callStatus = MutableStateFlow<String?>(null)
    val callStatus: StateFlow<String?> = _callStatus

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _voiceReady = MutableStateFlow(false)
    val voiceReady: StateFlow<Boolean> = _voiceReady

    // Incoming call state
    private val _hasIncomingCall = MutableStateFlow(false)
    val hasIncomingCall: StateFlow<Boolean> = _hasIncomingCall

    private val _incomingCallSid = MutableStateFlow<String?>(null)
    val incomingCallSid: StateFlow<String?> = _incomingCallSid

    private val _incomingCallFrom = MutableStateFlow<String?>(null)
    val incomingCallFrom: StateFlow<String?> = _incomingCallFrom

    private val _incomingCallerName = MutableStateFlow<String?>(null)
    val incomingCallerName: StateFlow<String?> = _incomingCallerName

    private val _incomingConferenceName = MutableStateFlow<String?>(null)

    // Dashboard
    private val _dashStats = MutableStateFlow<DashboardStats?>(null)
    val dashStats: StateFlow<DashboardStats?> = _dashStats

    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard

    private val _dashLoading = MutableStateFlow(false)
    val dashLoading: StateFlow<Boolean> = _dashLoading

    // Call History
    private val _callLogs = MutableStateFlow<List<CallLog>>(emptyList())
    val callLogs: StateFlow<List<CallLog>> = _callLogs

    private val _callsLoading = MutableStateFlow(false)
    val callsLoading: StateFlow<Boolean> = _callsLoading

    private val _callSearchQuery = MutableStateFlow("")
    val callSearchQuery: StateFlow<String> = _callSearchQuery

    private val _callFilter = MutableStateFlow<String?>(null)
    val callFilter: StateFlow<String?> = _callFilter

    private var callPage = 1

    // Contacts
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _contactsLoading = MutableStateFlow(false)
    val contactsLoading: StateFlow<Boolean> = _contactsLoading

    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery

    private var contactPage = 1

    // Phone Lists
    private val _phoneLists = MutableStateFlow<List<PhoneList>>(emptyList())
    val phoneLists: StateFlow<List<PhoneList>> = _phoneLists

    private val _selectedPhoneList = MutableStateFlow<PhoneList?>(null)
    val selectedPhoneList: StateFlow<PhoneList?> = _selectedPhoneList

    private val _phoneListEntries = MutableStateFlow<List<PhoneListEntry>>(emptyList())
    val phoneListEntries: StateFlow<List<PhoneListEntry>> = _phoneListEntries

    private val _powerDialProgress = MutableStateFlow<PowerDialProgress?>(null)
    val powerDialProgress: StateFlow<PowerDialProgress?> = _powerDialProgress

    private val _listsLoading = MutableStateFlow(false)
    val listsLoading: StateFlow<Boolean> = _listsLoading

    // Email
    private val _emails = MutableStateFlow<List<Email>>(emptyList())
    val emails: StateFlow<List<Email>> = _emails

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private val _emailsLoading = MutableStateFlow(false)
    val emailsLoading: StateFlow<Boolean> = _emailsLoading

    private val _emailFolder = MutableStateFlow<String?>(null)
    val emailFolder: StateFlow<String?> = _emailFolder

    // Payment method
    private val _paymentMethod = MutableStateFlow<PaymentMethodInfo?>(null)
    val paymentMethod: StateFlow<PaymentMethodInfo?> = _paymentMethod

    private val _setupCardUrl = MutableStateFlow<String?>(null)
    val setupCardUrl: StateFlow<String?> = _setupCardUrl

    // Calling status
    private val _callingStatus = MutableStateFlow<CallingStatus?>(null)
    val callingStatus: StateFlow<CallingStatus?> = _callingStatus

    // My account
    private val _myMinutes = MutableStateFlow<MinutesSummary?>(null)
    val myMinutes: StateFlow<MinutesSummary?> = _myMinutes

    private val _myNumbers = MutableStateFlow<List<MyNumber>>(emptyList())
    val myNumbers: StateFlow<List<MyNumber>> = _myNumbers

    private val _purchaseHistory = MutableStateFlow<List<Purchase>>(emptyList())
    val purchaseHistory: StateFlow<List<Purchase>> = _purchaseHistory

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    val purchaseMessage: StateFlow<String?> = _purchaseMessage

    // Store — Numbers
    private val _availableNumbers = MutableStateFlow<List<AvailableNumber>>(emptyList())
    val availableNumbers: StateFlow<List<AvailableNumber>> = _availableNumbers

    private val _numbersLoading = MutableStateFlow(false)
    val numbersLoading: StateFlow<Boolean> = _numbersLoading

    private val _selectedNumberType = MutableStateFlow("local")
    val selectedNumberType: StateFlow<String> = _selectedNumberType

    // Store — Minutes
    private val _minutesPackages = MutableStateFlow<List<MinutesPackage>>(emptyList())
    val minutesPackages: StateFlow<List<MinutesPackage>> = _minutesPackages

    private val _packagesLoading = MutableStateFlow(false)
    val packagesLoading: StateFlow<Boolean> = _packagesLoading

    // Attendance
    private val _attendance = MutableStateFlow<AttendanceSession?>(null)
    val attendance: StateFlow<AttendanceSession?> = _attendance

    init {
        val savedUrl = tokenManager.getBaseUrl()
        if (savedUrl != null) {
            RetrofitClient.init(tokenManager, savedUrl)
        } else {
            RetrofitClient.init(tokenManager)
        }

        // Restore theme preference
        ThemeState.isDarkMode.value = tokenManager.isDarkMode()

        // Setup Twilio voice callback
        twilioVoice.setCallEventListener(object : TwilioVoiceManager.CallEventListener {
            override fun onCallConnecting(callSid: String?) {
                _callStatus.value = "Connecting..."
                _isOnCall.value = true
            }
            override fun onCallRinging(callSid: String?) {
                _callStatus.value = "Ringing..."
            }
            override fun onCallConnected(callSid: String?) {
                _callStatus.value = "Connected"
            }
            override fun onCallDisconnected(callSid: String?, error: String?) {
                _isOnCall.value = false
                _activeCallNumber.value = null
                _callStatus.value = null
                _isMuted.value = false
            }
            override fun onCallFailed(error: String?) {
                _isOnCall.value = false
                _activeCallNumber.value = null
                _callStatus.value = "Failed: ${error ?: "Unknown error"}"
                _isMuted.value = false
            }
        })

        // Setup Socket.IO for incoming calls
        socketManager.setIncomingCallListener(object : SocketManager.IncomingCallListener {
            override fun onIncomingCall(callSid: String, fromNumber: String, callerName: String?, conferenceName: String?) {
                if (_isOnCall.value) return
                _hasIncomingCall.value = true
                _incomingCallSid.value = callSid
                _incomingCallFrom.value = fromNumber
                _incomingCallerName.value = callerName
                _incomingConferenceName.value = conferenceName

                // Auto-dismiss after 35 seconds (server dial timeout is 30s)
                viewModelScope.launch {
                    kotlinx.coroutines.delay(35000)
                    if (_incomingCallSid.value == callSid) {
                        _hasIncomingCall.value = false
                        _incomingCallSid.value = null
                        _incomingCallFrom.value = null
                        _incomingCallerName.value = null
                        _incomingConferenceName.value = null
                    }
                }
            }
            override fun onCallMissed(callSid: String, fromNumber: String) {
                if (_incomingCallSid.value == callSid) {
                    _hasIncomingCall.value = false
                    _incomingCallSid.value = null
                    _incomingCallFrom.value = null
                    _incomingCallerName.value = null
                    _incomingConferenceName.value = null
                }
            }
            override fun onCallEnded() {
                _hasIncomingCall.value = false
                _incomingCallSid.value = null
                _incomingCallFrom.value = null
                _incomingCallerName.value = null
                _incomingConferenceName.value = null
            }
        })

        if (tokenManager.isLoggedIn()) {
            loadProfile()
            loadCallingStatus()
        }
    }

    // === SOCKET ===
    private fun connectSocket() {
        val baseUrl = tokenManager.getBaseUrl() ?: RetrofitClient.BASE_URL
        val token = tokenManager.getToken() ?: return
        socketManager.connect(baseUrl, token)
    }

    // === INCOMING CALLS ===
    fun acceptIncomingCall() {
        val callSid = _incomingCallSid.value
        val fromNumber = _incomingCallFrom.value ?: return

        // Clear incoming UI
        _hasIncomingCall.value = false
        _incomingCallSid.value = null
        _incomingCallFrom.value = null
        _incomingCallerName.value = null
        _incomingConferenceName.value = null

        _activeCallNumber.value = fromNumber
        _callStatus.value = "Answering..."
        _isOnCall.value = true

        if (_twilioAccessToken != null) {
            viewModelScope.launch {
                try {
                    // Tell server to redirect the inbound call into a conference
                    val body = mutableMapOf<String, String>()
                    if (callSid != null) body["callSid"] = callSid
                    body["fromNumber"] = fromNumber
                    val resp = RetrofitClient.getApi().acceptInboundMobile(body)
                    if (resp.isSuccessful) {
                        val confName = resp.body()?.get("conferenceName")
                        if (confName != null) {
                            // Join the conference — this answers the actual call
                            twilioVoice.joinConference(confName, _twilioAccessToken!!)
                        } else {
                            _callStatus.value = "Failed: no conference returned"
                            _isOnCall.value = false
                            _activeCallNumber.value = null
                        }
                    } else {
                        Log.e("MainViewModel", "accept-mobile failed: ${resp.code()}")
                        _callStatus.value = "Failed: server error ${resp.code()}"
                        _isOnCall.value = false
                        _activeCallNumber.value = null
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error accepting: ${e.message}")
                    _callStatus.value = "Failed: ${e.message?.take(40)}"
                    _isOnCall.value = false
                    _activeCallNumber.value = null
                }
            }
        } else {
            _callStatus.value = "Voice not ready"
            _isOnCall.value = false
            _activeCallNumber.value = null
        }
    }

    fun rejectIncomingCall() {
        _hasIncomingCall.value = false
        _incomingCallSid.value = null
        _incomingCallFrom.value = null
        _incomingCallerName.value = null
        _incomingConferenceName.value = null
    }

    // === THEME ===
    fun toggleTheme() {
        val newValue = !ThemeState.isDarkMode.value
        ThemeState.isDarkMode.value = newValue
        tokenManager.saveDarkMode(newValue)
    }

    // === TWILIO VOICE ===
    private var _twilioAccessToken: String? = null

    fun fetchTwilioToken() {
        viewModelScope.launch {
            try {
                // Try customer token endpoint first
                val custResp = RetrofitClient.getApi().getCustomerTwilioToken()
                if (custResp.isSuccessful) {
                    val body = custResp.body()!!
                    _twilioAccessToken = body.token
                    twilioVoice.updateToken(body.token)
                    _voiceReady.value = true
                    Log.d("MainViewModel", "Customer Twilio token fetched for identity: ${body.identity}")
                    return@launch
                }
                // Fallback to agent token endpoint
                val resp = RetrofitClient.getApi().getTwilioToken()
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    _twilioAccessToken = body.token
                    twilioVoice.updateToken(body.token)
                    _voiceReady.value = true
                    Log.d("MainViewModel", "Twilio token fetched for identity: ${body.identity}")
                } else {
                    Log.e("MainViewModel", "Failed to fetch Twilio token: ${resp.code()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching Twilio token: ${e.message}")
            }
        }
    }

    fun loadCallingStatus() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().getCallingStatus()
                if (resp.isSuccessful) {
                    _callingStatus.value = resp.body()
                    // Auto-fetch Twilio token if customer has a number
                    if (resp.body()?.hasNumber == true) {
                        fetchTwilioToken()
                        connectSocket()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun makeCall(number: String) {
        try {
            if (!twilioVoice.hasMicPermission()) {
                _callStatus.value = "Microphone permission required"
                return
            }
            _activeCallNumber.value = number
            _callStatus.value = "Initiating call..."
            val success = twilioVoice.makeCall(number)
            if (!success) {
                _callStatus.value = "Voice not ready, retrying..."
                viewModelScope.launch {
                    fetchTwilioToken()
                    kotlinx.coroutines.delay(2000)
                    _activeCallNumber.value = number
                    try {
                        twilioVoice.makeCall(number)
                    } catch (e: Exception) {
                        _callStatus.value = "Failed: ${e.message?.take(50)}"
                        _isOnCall.value = false
                        _activeCallNumber.value = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "makeCall error: ${e.message}", e)
            _callStatus.value = "Failed: ${e.message?.take(50)}"
            _isOnCall.value = false
            _activeCallNumber.value = null
        }
    }

    fun hangup() {
        twilioVoice.hangup()
        _isOnCall.value = false
        _activeCallNumber.value = null
        _callStatus.value = null
        _isMuted.value = false
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        twilioVoice.mute(newMuted)
        _isMuted.value = newMuted
    }

    fun sendDtmf(digits: String) {
        twilioVoice.sendDigits(digits)
    }

    // === AUTH (Customer-based) ===
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginLoading.value = true
            _loginError.value = null
            try {
                val response = RetrofitClient.getApi().customerLogin(CustomerLoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenManager.saveToken(body.accessToken)
                    tokenManager.saveRefreshToken(body.refreshToken)
                    tokenManager.saveCustomerInfo(body.customer.id, body.customer.email, body.customer.name)
                    _isLoggedIn.value = true
                    loadDashboard()
                    loadCallingStatus()
                } else {
                    val errBody = response.errorBody()?.string()
                    if (errBody?.contains("suspended") == true) {
                        _loginError.value = "Account suspended. Contact support."
                    } else {
                        _loginError.value = "Invalid credentials"
                    }
                }
            } catch (e: Exception) {
                _loginError.value = "Connection failed: ${e.message?.take(60)}"
            } finally {
                _loginLoading.value = false
            }
        }
    }

    fun signup(email: String, password: String, name: String) {
        viewModelScope.launch {
            _loginLoading.value = true
            _loginError.value = null
            try {
                val response = RetrofitClient.getApi().customerSignup(CustomerSignupRequest(email, password, name.ifBlank { null }))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenManager.saveToken(body.accessToken)
                    tokenManager.saveRefreshToken(body.refreshToken)
                    tokenManager.saveCustomerInfo(body.customer.id, body.customer.email, body.customer.name)
                    _isLoggedIn.value = true
                    loadDashboard()
                    loadCallingStatus()
                } else {
                    val code = response.code()
                    if (code == 409) {
                        _loginError.value = "Email already registered"
                    } else {
                        _loginError.value = "Signup failed"
                    }
                }
            } catch (e: Exception) {
                _loginError.value = "Connection failed: ${e.message?.take(60)}"
            } finally {
                _loginLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { RetrofitClient.getApi().logout() } catch (_: Exception) { }
            twilioVoice.destroy()
            socketManager.disconnect()
            _voiceReady.value = false
            _twilioAccessToken = null
            tokenManager.clearAll()
            _isLoggedIn.value = false
            _agent.value = null
        }
    }

    // === PROFILE ===
    fun loadProfile() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApi().getMyProfile()
                if (response.isSuccessful) {
                    _agent.value = response.body()
                }
            } catch (_: Exception) { }
        }
    }

    // === DASHBOARD ===
    fun loadDashboard() {
        viewModelScope.launch {
            _dashLoading.value = true
            try {
                val statsResp = RetrofitClient.getApi().getCallStats()
                if (statsResp.isSuccessful) _dashStats.value = statsResp.body()

                val todayResp = RetrofitClient.getApi().getTodayCallCount()
                if (todayResp.isSuccessful) _todayCount.value = todayResp.body()?.count ?: 0

                val lbResp = RetrofitClient.getApi().getLeaderboard()
                if (lbResp.isSuccessful) _leaderboard.value = lbResp.body() ?: emptyList()
            } catch (_: Exception) { }
            _dashLoading.value = false
        }
    }

    // === AGENT STATUS ===
    fun updateStatus(status: String) {
        viewModelScope.launch {
            val agentId = _agent.value?.id ?: return@launch
            try {
                val resp = RetrofitClient.getApi().updateAgentStatus(agentId, AgentStatusUpdate(status))
                if (resp.isSuccessful) _agent.value = resp.body()
            } catch (_: Exception) { }
        }
    }

    // === CALL HISTORY ===
    fun loadCallLogs(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                callPage = 1
                _callLogs.value = emptyList()
            }
            _callsLoading.value = true
            try {
                val resp = RetrofitClient.getApi().getCallLogs(
                    page = callPage,
                    search = _callSearchQuery.value.ifBlank { null },
                    direction = _callFilter.value
                )
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    _callLogs.value = if (reset) body.calls else _callLogs.value + body.calls
                    callPage++
                }
            } catch (_: Exception) { }
            _callsLoading.value = false
        }
    }

    fun setCallSearch(query: String) {
        _callSearchQuery.value = query
        loadCallLogs(reset = true)
    }

    fun setCallFilter(filter: String?) {
        _callFilter.value = filter
        loadCallLogs(reset = true)
    }

    // === CONTACTS ===
    fun loadContacts(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                contactPage = 1
                _contacts.value = emptyList()
            }
            _contactsLoading.value = true
            try {
                val resp = RetrofitClient.getApi().getContacts(
                    page = contactPage,
                    search = _contactSearchQuery.value.ifBlank { null }
                )
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    _contacts.value = if (reset) body.contacts else _contacts.value + body.contacts
                    contactPage++
                }
            } catch (_: Exception) { }
            _contactsLoading.value = false
        }
    }

    fun setContactSearch(query: String) {
        _contactSearchQuery.value = query
        loadContacts(reset = true)
    }

    fun createContact(contact: ContactCreate) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().createContact(contact)
                if (resp.isSuccessful) loadContacts(reset = true)
            } catch (_: Exception) { }
        }
    }

    fun toggleFavorite(contactId: Int) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().toggleFavorite(contactId)
                if (resp.isSuccessful) {
                    _contacts.value = _contacts.value.map {
                        if (it.id == contactId) resp.body()!! else it
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun deleteContact(contactId: Int) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().deleteContact(contactId)
                if (resp.isSuccessful) {
                    _contacts.value = _contacts.value.filter { it.id != contactId }
                }
            } catch (_: Exception) { }
        }
    }

    // === PHONE LISTS ===
    fun loadPhoneLists() {
        viewModelScope.launch {
            _listsLoading.value = true
            try {
                val resp = RetrofitClient.getApi().getPhoneLists()
                if (resp.isSuccessful) _phoneLists.value = resp.body() ?: emptyList()
            } catch (_: Exception) { }
            _listsLoading.value = false
        }
    }

    fun selectPhoneList(list: PhoneList?) {
        _selectedPhoneList.value = list
        if (list != null) {
            loadPhoneListEntries(list.id)
            loadPowerDialProgress(list.id)
        } else {
            _phoneListEntries.value = emptyList()
            _powerDialProgress.value = null
        }
    }

    private fun loadPhoneListEntries(listId: Int) {
        viewModelScope.launch {
            _listsLoading.value = true
            try {
                val resp = RetrofitClient.getApi().getPhoneListEntries(listId)
                if (resp.isSuccessful) _phoneListEntries.value = resp.body()?.entries ?: emptyList()
            } catch (_: Exception) { }
            _listsLoading.value = false
        }
    }

    private fun loadPowerDialProgress(listId: Int) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().getPowerDialProgress(listId)
                if (resp.isSuccessful) _powerDialProgress.value = resp.body()
            } catch (_: Exception) { }
        }
    }

    fun updateEntryStatus(entryId: Int, status: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().updateEntryStatus(entryId, mapOf("status" to status))
                if (resp.isSuccessful) {
                    _phoneListEntries.value = _phoneListEntries.value.map {
                        if (it.id == entryId) resp.body()!! else it
                    }
                    _selectedPhoneList.value?.let { loadPowerDialProgress(it.id) }
                }
            } catch (_: Exception) { }
        }
    }

    fun deletePhoneList(listId: Int) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().deletePhoneList(listId)
                if (resp.isSuccessful) {
                    _phoneLists.value = _phoneLists.value.filter { it.id != listId }
                }
            } catch (_: Exception) { }
        }
    }

    // === EMAIL ===
    fun loadEmails(folder: String? = null) {
        viewModelScope.launch {
            _emailsLoading.value = true
            _emailFolder.value = folder
            try {
                val resp = RetrofitClient.getApi().getInbox(folder = folder)
                if (resp.isSuccessful) _emails.value = resp.body()?.emails ?: emptyList()

                val unreadResp = RetrofitClient.getApi().getUnreadCount()
                if (unreadResp.isSuccessful) _unreadCount.value = unreadResp.body()?.get("count") ?: 0
            } catch (_: Exception) { }
            _emailsLoading.value = false
        }
    }

    fun syncEmails() {
        viewModelScope.launch {
            _emailsLoading.value = true
            try {
                loadEmails(_emailFolder.value)
            } catch (_: Exception) { }
            _emailsLoading.value = false
        }
    }

    // === ATTENDANCE ===
    fun loadAttendance() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().getCurrentAttendance()
                if (resp.isSuccessful) _attendance.value = resp.body()
            } catch (_: Exception) { }
        }
    }

    fun clockIn() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().clockIn()
                if (resp.isSuccessful) loadAttendance()
            } catch (_: Exception) { }
        }
    }

    fun clockOut() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().clockOut()
                if (resp.isSuccessful) loadAttendance()
            } catch (_: Exception) { }
        }
    }

    // === STORE ===
    fun loadAvailableNumbers(type: String = "local") {
        viewModelScope.launch {
            _numbersLoading.value = true
            _selectedNumberType.value = type
            try {
                val resp = RetrofitClient.getApi().getAvailableNumbers(type = type)
                if (resp.isSuccessful) {
                    _availableNumbers.value = resp.body()?.numbers ?: emptyList()
                }
            } catch (_: Exception) { }
            _numbersLoading.value = false
        }
    }

    fun refreshNumbers() {
        loadAvailableNumbers(_selectedNumberType.value)
    }

    fun loadCustomerCalls() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().getMyCallHistory(page = 1, limit = 10)
                if (resp.isSuccessful) {
                    _customerCalls.value = resp.body()?.calls ?: emptyList()
                }
            } catch (_: Exception) { }
        }
    }

    fun loadPaymentMethod() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().getPaymentMethod()
                if (resp.isSuccessful) _paymentMethod.value = resp.body()
            } catch (_: Exception) { }
        }
    }

    fun setupCard() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().setupCard()
                if (resp.isSuccessful) {
                    _setupCardUrl.value = resp.body()?.url
                } else {
                    val err = resp.errorBody()?.string()
                    _purchaseMessage.value = if (err?.contains("not configured") == true) "Payment system coming soon!" else "Failed to setup card"
                    kotlinx.coroutines.delay(3000)
                    _purchaseMessage.value = null
                }
            } catch (e: Exception) {
                _purchaseMessage.value = "Error: ${e.message?.take(40)}"
                kotlinx.coroutines.delay(3000)
                _purchaseMessage.value = null
            }
        }
    }

    fun clearSetupCardUrl() { _setupCardUrl.value = null }

    fun purchaseWithCard(type: String, itemId: String?, itemLabel: String?, amount: Double?, metadata: Map<String, Any?>?) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().chargeCard(ChargeRequest(type, itemId, itemLabel, amount, metadata))
                if (resp.isSuccessful) {
                    _purchaseMessage.value = resp.body()?.message ?: "Purchase successful!"
                    loadMyAccount()
                    loadPaymentMethod()
                } else {
                    val code = resp.code()
                    val err = resp.errorBody()?.string()
                    _purchaseMessage.value = when {
                        code == 402 -> "Card declined"
                        code == 400 && err?.contains("No card") == true -> "Please add a card first"
                        code == 503 -> "Payment not available yet. Use mock purchase."
                        else -> "Purchase failed"
                    }
                }
            } catch (e: Exception) {
                _purchaseMessage.value = "Error: ${e.message?.take(40)}"
            }
            kotlinx.coroutines.delay(3000)
            _purchaseMessage.value = null
        }
    }

    fun mockPurchaseMinutes(pkg: MinutesPackage) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().mockPurchase(MockPurchaseRequest(
                    type = "minutes",
                    itemId = pkg.id,
                    itemLabel = "${pkg.minutes} Minutes",
                    amount = pkg.price,
                    metadata = mapOf("minutes" to pkg.minutes)
                ))
                if (resp.isSuccessful) {
                    _purchaseMessage.value = "Purchased ${pkg.minutes} minutes!"
                    loadMyAccount()
                } else {
                    _purchaseMessage.value = "Purchase failed"
                }
            } catch (e: Exception) {
                _purchaseMessage.value = "Error: ${e.message?.take(40)}"
            }
            // Auto-clear message
            kotlinx.coroutines.delay(3000)
            _purchaseMessage.value = null
        }
    }

    fun mockPurchaseNumber(number: AvailableNumber) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().mockPurchase(MockPurchaseRequest(
                    type = "number",
                    itemId = number.phoneNumber,
                    itemLabel = number.phoneNumber ?: "Phone Number",
                    amount = number.monthlyPrice,
                    metadata = mapOf(
                        "phoneNumber" to number.phoneNumber,
                        "friendlyName" to number.friendlyName,
                        "numberType" to number.type,
                        "monthlyPrice" to number.monthlyPrice
                    )
                ))
                if (resp.isSuccessful) {
                    _purchaseMessage.value = "Purchased ${number.phoneNumber}!"
                    loadMyAccount()
                } else {
                    _purchaseMessage.value = "Purchase failed"
                }
            } catch (e: Exception) {
                _purchaseMessage.value = "Error: ${e.message?.take(40)}"
            }
            kotlinx.coroutines.delay(3000)
            _purchaseMessage.value = null
        }
    }

    fun loadMyAccount() {
        viewModelScope.launch {
            try {
                val minResp = RetrofitClient.getApi().getMyMinutes()
                if (minResp.isSuccessful) _myMinutes.value = minResp.body()
                val numResp = RetrofitClient.getApi().getMyNumbers()
                if (numResp.isSuccessful) _myNumbers.value = numResp.body()?.numbers ?: emptyList()
                val histResp = RetrofitClient.getApi().getPurchaseHistory()
                if (histResp.isSuccessful) _purchaseHistory.value = histResp.body()?.purchases ?: emptyList()
            } catch (_: Exception) { }
        }
    }

    fun selectNumber(phoneNumber: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.getApi().selectNumber(SelectNumberRequest(phoneNumber))
                if (resp.isSuccessful) {
                    _callingStatus.value = _callingStatus.value?.copy(phoneNumber = phoneNumber)
                    // Re-fetch token for the new number's identity
                    fetchTwilioToken()
                    _purchaseMessage.value = "Switched to $phoneNumber"
                    kotlinx.coroutines.delay(2000)
                    _purchaseMessage.value = null
                }
            } catch (_: Exception) { }
        }
    }

    fun loadMinutesPackages() {
        viewModelScope.launch {
            _packagesLoading.value = true
            try {
                val resp = RetrofitClient.getApi().getMinutesPackages()
                if (resp.isSuccessful) {
                    _minutesPackages.value = resp.body()?.packages ?: emptyList()
                }
            } catch (_: Exception) { }
            _packagesLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        twilioVoice.destroy()
        socketManager.disconnect()
    }
}
