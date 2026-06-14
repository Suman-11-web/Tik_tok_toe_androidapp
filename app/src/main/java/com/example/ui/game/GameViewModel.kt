package com.example.ui.game

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynth
import com.example.data.database.GameDatabase
import com.example.data.database.MatchRecord
import com.example.data.database.ProfileStats
import com.example.data.network.MqttManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class AppState {
    MENU, LOBBY_SETUP, LOBBY_WAIT, GAME_PLAY, HISTORY, PROFILE
}

enum class GameMode {
    LOCAL, VS_AI, ONLINE
}

enum class NetStatus {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

data class FloatingEmoji(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String,
    val xOffset: Float = (20..80).random().toFloat() / 100f,
    val duration: Int = 1500
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GameDatabase.getDatabase(application)
    private val dao = db.gameDao()

    // Screen and state flows
    private val _appState = MutableStateFlow(AppState.MENU)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _gameMode = MutableStateFlow(GameMode.LOCAL)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    // Tic-Tac-Toe Game State
    private val _board = MutableStateFlow<List<String?>>(List(9) { null })
    val board: StateFlow<List<String?>> = _board.asStateFlow()

    private val _currentTurn = MutableStateFlow("X") // "X" or "O"
    val currentTurn: StateFlow<String> = _currentTurn.asStateFlow()

    private val _mySymbol = MutableStateFlow("X") // "X" or "O" (Only relevant in ONLINE mode)
    val mySymbol: StateFlow<String> = _mySymbol.asStateFlow()

    private val _winningLine = MutableStateFlow<List<Int>?>(null)
    val winningLine: StateFlow<List<Int>?> = _winningLine.asStateFlow()

    private val _gameResult = MutableStateFlow<String?>(null) // "WIN_X", "WIN_O", "DRAW", or null
    val gameResult: StateFlow<String?> = _gameResult.asStateFlow()

    // Multiplayer room codes and info
    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _opponentName = MutableStateFlow("Opponent")
    val opponentName: StateFlow<String> = _opponentName.asStateFlow()

    private val _opponentSymbol = MutableStateFlow("O")
    val opponentSymbol: StateFlow<String> = _opponentSymbol.asStateFlow()

    // Network status
    private val _networkStatus = MutableStateFlow(NetStatus.DISCONNECTED)
    val networkStatus: StateFlow<NetStatus> = _networkStatus.asStateFlow()

    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError.asStateFlow()

    // Profile & History (from SQLite db)
    val matchHistory: StateFlow<List<MatchRecord>> = dao.getAllMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profileStats: StateFlow<ProfileStats> = dao.getProfileStats()
        .map { it ?: ProfileStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileStats())

    // Live emoji chat list
    val activeEmojis = mutableStateListOf<FloatingEmoji>()

    // Rematch states
    private val _rematchProposedByMe = MutableStateFlow(false)
    val rematchProposedByMe: StateFlow<Boolean> = _rematchProposedByMe.asStateFlow()

    private val _rematchProposedByOpponent = MutableStateFlow(false)
    val rematchProposedByOpponent: StateFlow<Boolean> = _rematchProposedByOpponent.asStateFlow()

    // MQTT connection manager
    private var mqttManager: MqttManager? = null
    private var joinJob: kotlinx.coroutines.Job? = null

    init {
        // Pre-create user profile if it doesn't exist
        viewModelScope.launch(Dispatchers.IO) {
            dao.getProfileStats().collectLatest { stats ->
                if (stats == null) {
                    dao.updateProfileStats(ProfileStats())
                }
            }
        }
    }

    // Navigation and screen management
    fun navigateTo(state: AppState) {
        if (_appState.value == AppState.GAME_PLAY && state != AppState.GAME_PLAY) {
            // Disconnect or reset when leaving active game play
            if (_gameMode.value == GameMode.ONLINE) {
                mqttManager?.unsubscribeFromCurrentRoom()
            }
        }
        if (state == AppState.MENU) {
            mqttManager?.disconnect()
        }
        _appState.value = state
    }

    fun updateUsername(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = profileStats.value
            dao.updateProfileStats(current.copy(username = newName.take(15)))
        }
    }

    // Reset whole DB history
    fun clearStats() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearMatchHistory()
            val current = profileStats.value
            dao.updateProfileStats(current.copy(wins = 0, losses = 0, draws = 0, currentStreak = 0))
        }
    }

    // Sound and particle triggers
    fun sendLocalEmoji(emojiStr: String) {
        SoundSynth.playEmoji()
        val flowEmoji = FloatingEmoji(emoji = emojiStr)
        activeEmojis.add(flowEmoji)
        
        viewModelScope.launch {
            delay(1500)
            activeEmojis.remove(flowEmoji)
        }

        if (_gameMode.value == GameMode.ONLINE) {
            mqttManager?.sendMessage("EMOJI") {
                put("emoji", emojiStr)
            }
        }
    }

    // Create room code
    fun startOnlineMatchSetup() {
        _networkStatus.value = NetStatus.CONNECTING
        _networkError.value = null
        
        // Disconnect previous manager if any
        mqttManager?.disconnect()
        
        mqttManager = MqttManager(
            onMessageReceived = { type, senderId, data -> handleMqttMessage(type, senderId, data) },
            onConnectionStateChanged = { connected, error ->
                _networkStatus.value = if (connected) NetStatus.CONNECTED else NetStatus.DISCONNECTED
                error?.let {
                    _networkError.value = it
                    _networkStatus.value = NetStatus.FAILED
                }
            }
        )
        mqttManager?.connect()
        navigateTo(AppState.LOBBY_SETUP)
    }

    fun reconnectNetwork() {
        _networkStatus.value = NetStatus.CONNECTING
        _networkError.value = null
        if (mqttManager == null) {
            mqttManager = MqttManager(
                onMessageReceived = { type, senderId, data -> handleMqttMessage(type, senderId, data) },
                onConnectionStateChanged = { connected, error ->
                    _networkStatus.value = if (connected) NetStatus.CONNECTED else NetStatus.DISCONNECTED
                    error?.let {
                        _networkError.value = it
                        _networkStatus.value = NetStatus.FAILED
                    }
                }
            )
        }
        mqttManager?.connect()
    }

    fun hostOnlineRoom() {
        val randomCode = (100000..999999).random().toString()
        _roomCode.value = randomCode
        _mySymbol.value = "X"
        _opponentSymbol.value = "O"
        _opponentName.value = "Searching..."
        
        _gameMode.value = GameMode.ONLINE
        mqttManager?.subscribeToRoom(randomCode)
        _appState.value = AppState.LOBBY_WAIT
        
        // Host broadcast existence periodically
        viewModelScope.launch {
            while (_appState.value == AppState.LOBBY_WAIT) {
                if (mqttManager?.isConnected() == true) {
                    mqttManager?.sendMessage("LOBBY_WAIT") {
                        put("hostName", profileStats.value.username)
                    }
                }
                delay(2000)
            }
        }
    }

    fun joinOnlineRoom(codeParam: String) {
        val code = codeParam.trim()
        if (code.length != 6) {
            _networkError.value = "Match Room Code must be exactly 6 digits!"
            return
        }
        _roomCode.value = code
        _mySymbol.value = "O"
        _opponentSymbol.value = "X"
        _gameMode.value = GameMode.ONLINE
        _opponentName.value = "Connecting..."
        
        mqttManager?.subscribeToRoom(code)
        
        // Cancel any pending handshake loop to prevent duplicates
        joinJob?.cancel()
        joinJob = viewModelScope.launch {
            delay(500) // brief respite while subscription goes through
            
            // Loop until we obtain the host's actual nickname (i.e. we are no longer "Connecting...")
            while (_gameMode.value == GameMode.ONLINE && _mySymbol.value == "O" && _appState.value == AppState.GAME_PLAY) {
                if (mqttManager?.isConnected() == true) {
                    Log.d("GameViewModel", "Handshake: Broadcasting JOIN for room $code...")
                    mqttManager?.sendMessage("JOIN") {
                        put("nickname", profileStats.value.username)
                    }
                }
                
                if (_opponentName.value != "Connecting..." && _opponentName.value != "Opponent" && _opponentName.value != "Searching...") {
                    Log.d("GameViewModel", "Handshake finalized! Connected to host: ${_opponentName.value}")
                    break
                }
                
                delay(1500)
            }
        }
        
        _appState.value = AppState.GAME_PLAY
        startNewGame(runSync = false)
    }

    // Game controls
    fun selectOfflineMode(mode: GameMode) {
        _gameMode.value = mode
        navigateTo(AppState.GAME_PLAY)
        startNewGame()
    }

    fun startNewGame(runSync: Boolean = true) {
        _board.value = List(9) { null }
        _currentTurn.value = "X"
        _winningLine.value = null
        _gameResult.value = null
        _rematchProposedByMe.value = false
        _rematchProposedByOpponent.value = false

        if (runSync && _gameMode.value == GameMode.ONLINE) {
            mqttManager?.sendMessage("RESET") {
                put("reset", true)
            }
        }
    }

    // Try cell moves
    fun makeMove(cellIndex: Int) {
        if (_board.value[cellIndex] != null || _gameResult.value != null) return

        val symbolActive = _currentTurn.value

        // Check turn authorization in online matches
        if (_gameMode.value == GameMode.ONLINE) {
            if (symbolActive != _mySymbol.value) return // Not your turn!
            if (_opponentName.value == "Searching..." || _opponentName.value == "Connecting...") return
        }

        // Apply cell state
        val updatedBoard = _board.value.toMutableList()
        updatedBoard[cellIndex] = symbolActive
        _board.value = updatedBoard

        // Play snappy audio sound
        if (symbolActive == "X") SoundSynth.playClickX() else SoundSynth.playClickO()

        // Sync cell over cloud
        if (_gameMode.value == GameMode.ONLINE) {
            mqttManager?.sendMessage("MOVE") {
                put("cellIndex", cellIndex)
                put("symbol", symbolActive)
            }
        }

        // Evaluate win conditions
        if (evaluateGame(updatedBoard, symbolActive)) return

        // Turn switching & triggers
        val nextSymbol = if (symbolActive == "X") "O" else "X"
        _currentTurn.value = nextSymbol

        if (_gameMode.value == GameMode.VS_AI && nextSymbol == "O") {
            viewModelScope.launch {
                delay((400..800).random().toLong()) // Dynamic delay to simulate thinking!
                makeAiOptimalMove()
            }
        }
    }

    // Real-time MQTT Match synchronization callbacks
    private fun handleMqttMessage(type: String, senderId: String, data: JSONObject) {
        viewModelScope.launch(Dispatchers.Main) {
            when (type) {
                "LOBBY_WAIT" -> {
                    // Host periodically announces room. If we are waiting, register host details
                    if (_gameMode.value == GameMode.ONLINE && _mySymbol.value == "O" && _appState.value == AppState.LOBBY_WAIT) {
                        _opponentName.value = data.optString("hostName", "Host")
                    }
                }
                "JOIN" -> {
                    // Player 2 joined our hosted room!
                    if (_gameMode.value == GameMode.ONLINE && _mySymbol.value == "X") {
                        val joineeName = data.optString("nickname", "Opponent")
                        val isNewMatch = _appState.value != AppState.GAME_PLAY
                        
                        _opponentName.value = joineeName
                        
                        // Affirm back to Joinee with our Host username
                        mqttManager?.sendMessage("AFFIRM") {
                            put("hostName", profileStats.value.username)
                        }
                        
                        if (isNewMatch) {
                            SoundSynth.playVictory() // Play chime when player connects
                            _appState.value = AppState.GAME_PLAY
                            startNewGame(runSync = false)
                        }
                    }
                }
                "AFFIRM" -> {
                    // Receiver receives host's nickname affirmation
                    if (_gameMode.value == GameMode.ONLINE && _mySymbol.value == "O") {
                        val hostName = data.optString("hostName", "Opponent")
                        if (_opponentName.value == "Connecting..." || _opponentName.value == "Opponent" || _opponentName.value == "Searching...") {
                            _opponentName.value = hostName
                            SoundSynth.playVictory()
                        } else {
                            _opponentName.value = hostName
                        }
                    }
                }
                "MOVE" -> {
                    val cellIndex = data.optInt("cellIndex", -1)
                    val symbolObj = data.optString("symbol", "")
                    
                    if (cellIndex in 0..8 && symbolObj == _currentTurn.value) {
                        if (_board.value[cellIndex] == null && _gameResult.value == null) {
                            val updatedBoard = _board.value.toMutableList()
                            updatedBoard[cellIndex] = symbolObj
                            _board.value = updatedBoard

                            if (symbolObj == "X") SoundSynth.playClickX() else SoundSynth.playClickO()
                            
                            if (!evaluateGame(updatedBoard, symbolObj)) {
                                _currentTurn.value = if (symbolObj == "X") "O" else "X"
                            }
                        }
                    }
                }
                "EMOJI" -> {
                    val chatEmoji = data.optString("emoji", "🔥")
                    val flowEmoji = FloatingEmoji(emoji = chatEmoji)
                    activeEmojis.add(flowEmoji)
                    SoundSynth.playEmoji()
                    
                    viewModelScope.launch {
                        delay(1500)
                        activeEmojis.remove(flowEmoji)
                    }
                }
                "RESET" -> {
                    startNewGame(runSync = false)
                }
                "REMATCH" -> {
                    val actVal = data.optString("action", "")
                    if (actVal == "PROPOSE") {
                        _rematchProposedByOpponent.value = true
                    } else if (actVal == "ACCEPT") {
                        _rematchProposedByOpponent.value = false
                        startNewGame(runSync = false)
                    }
                }
            }
        }
    }

    // Rematch triggers
    fun proposeRematch() {
        if (_gameMode.value != GameMode.ONLINE) {
            startNewGame()
            return
        }

        _rematchProposedByMe.value = true
        mqttManager?.sendMessage("REMATCH") {
            put("action", "PROPOSE")
        }

        // Auto-join if teammate already proposed it
        if (_rematchProposedByOpponent.value) {
            acceptRematch()
        }
    }

    fun acceptRematch() {
        _rematchProposedByMe.value = false
        _rematchProposedByOpponent.value = false
        startNewGame(runSync = false)

        if (_gameMode.value == GameMode.ONLINE) {
            mqttManager?.sendMessage("REMATCH") {
                put("action", "ACCEPT")
            }
        }
    }

    // Game evaluation
    private fun evaluateGame(boardState: List<String?>, activeSymbol: String): Boolean {
        val winningCombos = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Rows
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columns
            listOf(0, 4, 8), listOf(2, 4, 6)                  // Diagonals
        )

        for (combo in winningCombos) {
            if (boardState[combo[0]] == activeSymbol &&
                boardState[combo[1]] == activeSymbol &&
                boardState[combo[2]] == activeSymbol
            ) {
                _winningLine.value = combo
                _gameResult.value = if (activeSymbol == "X") "WIN_X" else "WIN_O"
                
                // Track stats based on game outcomes
                persistStats(activeSymbol)
                return true
            }
        }

        if (boardState.none { it == null }) {
            _gameResult.value = "DRAW"
            persistStats("DRAW")
            return true
        }

        return false
    }

    private fun persistStats(resultSymbol: String) {
        val myActiveSymbol = if (_gameMode.value == GameMode.ONLINE) _mySymbol.value else "X"
        val opponentActiveName = when (_gameMode.value) {
            GameMode.ONLINE -> _opponentName.value
            GameMode.VS_AI -> "Gemini AI"
            GameMode.LOCAL -> "Local Friend"
        }

        val outcomeStr = if (resultSymbol == "DRAW") {
            SoundSynth.playTone(392.0, 350, "sine") // Muted draw chime
            "DRAW"
        } else if (resultSymbol == myActiveSymbol) {
            SoundSynth.playVictory()
            "WIN"
        } else {
            SoundSynth.playDefeat()
            "LOSS"
        }

        // Save Match Record
        val record = MatchRecord(
            mode = _gameMode.value.name,
            roomCode = _roomCode.value,
            opponentName = opponentActiveName,
            playerSymbol = myActiveSymbol,
            outcome = outcomeStr
        )

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMatch(record)
            
            val current = profileStats.value
            val updateStats = when (outcomeStr) {
                "WIN" -> current.copy(
                    wins = current.wins + 1,
                    currentStreak = current.currentStreak + 1
                )
                "LOSS" -> current.copy(
                    losses = current.losses + 1,
                    currentStreak = 0
                )
                else -> current.copy(
                    draws = current.draws + 1
                )
            }
            dao.updateProfileStats(updateStats)
        }
    }

    // AI Minimax Player
    private fun makeAiOptimalMove() {
        val currentBoard = _board.value
        if (_gameResult.value != null) return

        // 8% chance of making a completely random move to keep AI human-like and fun
        if ((1..100).random() <= 8) {
            val emptyCells = currentBoard.indices.filter { currentBoard[it] == null }
            if (emptyCells.isNotEmpty()) {
                makeMove(emptyCells.random())
                return
            }
        }

        var bestScore = Int.MIN_VALUE
        var bestMove = -1

        for (i in 0..8) {
            if (currentBoard[i] == null) {
                val tempBoard = currentBoard.toMutableList()
                tempBoard[i] = "O" // AI is always O in VS_AI
                val score = minimax(tempBoard, 0, false)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = i
                }
            }
        }

        if (bestMove != -1) {
            makeMove(bestMove)
        }
    }

    private fun minimax(boardState: List<String?>, depth: Int, isMaximizing: Boolean): Int {
        val winStatus = checkBoardWinner(boardState)
        if (winStatus == "O") return 10 - depth // Minimizes depth to win quickly
        if (winStatus == "X") return depth - 10 // Pushed to block player as far back as possible
        if (boardState.none { it == null }) return 0

        if (isMaximizing) {
            var bestScore = Int.MIN_VALUE
            for (i in 0..8) {
                if (boardState[i] == null) {
                    val temp = boardState.toMutableList()
                    temp[i] = "O"
                    val score = minimax(temp, depth + 1, false)
                    bestScore = maxOf(bestScore, score)
                }
            }
            return bestScore
        } else {
            var bestScore = Int.MAX_VALUE
            for (i in 0..8) {
                if (boardState[i] == null) {
                    val temp = boardState.toMutableList()
                    temp[i] = "X"
                    val score = minimax(temp, depth + 1, true)
                    bestScore = minOf(bestScore, score)
                }
            }
            return bestScore
        }
    }

    private fun checkBoardWinner(boardState: List<String?>): String? {
        val combos = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (combo in combos) {
            if (boardState[combo[0]] != null &&
                boardState[combo[0]] == boardState[combo[1]] &&
                boardState[combo[0]] == boardState[combo[2]]
            ) {
                return boardState[combo[0]]
            }
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        mqttManager?.disconnect()
    }
}
