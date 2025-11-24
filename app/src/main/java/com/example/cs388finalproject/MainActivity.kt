package com.example.cs388finalproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.cs388finalproject.databinding.ActivityMainBinding
import com.example.cs388finalproject.ProfileFragment
import com.example.cs388finalproject.ui.auth.LoginActivity
import com.example.cs388finalproject.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// ---------- DATA MODELS ----------

data class SpotifyProfile(
    val id: String,
    val displayName: String,
    val imageUrl: String?
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artist: String?,
    val albumName: String?,
    val imageUrl: String?,
    val durationMs: Int = 0,
    val explicit: Boolean = false,
    val previewUrl: String? = null
)

data class SpotifyState(
    val profile: SpotifyProfile,
    val topTracks: List<SpotifyTrack>,
    val currentTrack: SpotifyTrack?
)

// ---------- MAIN ACTIVITY ----------

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    lateinit var locationHelper: LocationHelper

    private val CLIENT_ID = "c1844ea7fb444fbe97d3bb3bce4bf19b"
    private val REDIRECT_URI = "musicmedia://callback"
    private val REQUEST_CODE = 1337

    private var spotifyAccessToken: String? = null
    private var spotifyState: SpotifyState? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)

        locationHelper = LocationHelper(this)
    }

    // ---------- SPOTIFY LOGIN ----------

    fun startSpotifyLogin() {
        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        )

        builder.setScopes(
            arrayOf(
                "user-read-email",
                "user-read-private",
                "user-top-read",
                "user-read-currently-playing",
                "user-read-playback-state"
            )
        )

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, builder.build())
    }

    fun getSpotifyState(): SpotifyState? = spotifyState

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    spotifyAccessToken = response.accessToken
                    Log.d("Spotify", "Access Token: $spotifyAccessToken")

                    locationHelper.requestLocation()

                    // One-time profile + top tracks
                    fetchSpotifyProfileAndTopTracks()

                    // Current track + auto-refresh
                    fetchCurrentlyPlaying()
                    startAutoRefresh()
                }
                AuthorizationResponse.Type.ERROR ->
                    Log.e("Spotify", "Auth error: ${response.error}")
                else -> Log.e("Spotify", "Auth canceled")
            }
        }
    }

    private fun isJson(text: String): Boolean =
        text.trim().startsWith("{") || text.trim().startsWith("[")

    // ---------- PROFILE + TOP TRACKS ----------

    private fun fetchSpotifyProfileAndTopTracks() {
        val token = spotifyAccessToken ?: return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                // PROFILE
                val profileRes = client.newCall(
                    Request.Builder()
                        .url("https://api.spotify.com/v1/me")
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                ).execute()

                val profileBody = profileRes.body?.string().orEmpty()
                if (!isJson(profileBody)) return@launch

                val profileJson = JSONObject(profileBody)

                val profile = SpotifyProfile(
                    id = profileJson.optString("id", ""),
                    displayName = profileJson.optString("display_name", "Spotify User"),
                    imageUrl = profileJson.optJSONArray("images")
                        ?.optJSONObject(0)
                        ?.optString("url")
                )

                // TOP 3 TRACKS
                val tracksRes = client.newCall(
                    Request.Builder()
                        .url("https://api.spotify.com/v1/me/top/tracks?limit=3&time_range=medium_term")
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                ).execute()

                val topBody = tracksRes.body?.string().orEmpty()
                val topTracks = mutableListOf<SpotifyTrack>()

                if (isJson(topBody)) {
                    val json = JSONObject(topBody)
                    val items = json.optJSONArray("items")
                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val t = items.getJSONObject(i)
                            topTracks.add(
                                SpotifyTrack(
                                    id = t.optString("id"),
                                    name = t.optString("name"),
                                    artist = t.optJSONArray("artists")
                                        ?.optJSONObject(0)
                                        ?.optString("name"),
                                    albumName = t.optJSONObject("album")
                                        ?.optString("name"),
                                    imageUrl = t.optJSONObject("album")
                                        ?.optJSONArray("images")
                                        ?.optJSONObject(0)
                                        ?.optString("url"),
                                    durationMs = t.optInt("duration_ms"),
                                    explicit = t.optBoolean("explicit"),
                                    previewUrl = t.optString("preview_url")
                                )
                            )
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    val existingCurrent = spotifyState?.currentTrack
                    spotifyState = SpotifyState(
                        profile = profile,
                        topTracks = topTracks,
                        currentTrack = existingCurrent
                    )

                    // Update profile fragment UI if visible
                    val navHost =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val current =
                        navHost.childFragmentManager.primaryNavigationFragment
                    if (current is ProfileFragment) {
                        current.updateSpotifyUi(profile, topTracks, animate = false)
                    }
                }

            } catch (e: Exception) {
                Log.e("Spotify", "Error fetching profile/top tracks: ${e.message}")
            }
        }
    }

    // ---------- CURRENTLY PLAYING ----------

    private fun fetchCurrentlyPlaying() {
        val token = spotifyAccessToken ?: return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val res = client.newCall(
                    Request.Builder()
                        .url("https://api.spotify.com/v1/me/player/currently-playing")
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                ).execute()

                val body = res.body?.string().orEmpty()
                if (!isJson(body)) return@launch

                val json = JSONObject(body)
                val item = json.optJSONObject("item") ?: return@launch

                val track = SpotifyTrack(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    artist = item.optJSONArray("artists")
                        ?.optJSONObject(0)
                        ?.optString("name"),
                    albumName = item.optJSONObject("album")
                        ?.optString("name"),
                    imageUrl = item.optJSONObject("album")
                        ?.optJSONArray("images")
                        ?.optJSONObject(0)
                        ?.optString("url"),
                    durationMs = item.optInt("duration_ms"),
                    explicit = item.optBoolean("explicit"),
                    previewUrl = item.optString("preview_url")
                )

                Log.d("Spotify", "NOW PLAYING → ${track.name} — ${track.artist}")

                withContext(Dispatchers.Main) {
                    val existing = spotifyState
                    spotifyState = if (existing == null) {
                        SpotifyState(
                            profile = SpotifyProfile("", "Spotify User", null),
                            topTracks = emptyList(),
                            currentTrack = track
                        )
                    } else {
                        existing.copy(currentTrack = track)
                    }
                }

            } catch (e: Exception) {
                Log.e("Spotify", "Currently playing error: ${e.message}")
            }
        }
    }

    // ---------- AUTO REFRESH ----------

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = coroutineScope.launch {
            while (isActive) {
                fetchCurrentlyPlaying()
                delay(5000) // every 5 seconds
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        refreshJob?.cancel()
    }
}