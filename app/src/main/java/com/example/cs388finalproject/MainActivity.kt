package com.example.cs388finalproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.cs388finalproject.databinding.ActivityMainBinding
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

data class SpotifyProfile(
    val id: String,
    val displayName: String,
    val imageUrl: String?
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artist: String,
    val imageUrl: String?
)

data class SpotifyState(
    val profile: SpotifyProfile,
    val tracks: List<SpotifyTrack>
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var locationHelper: LocationHelper

    private val CLIENT_ID = "c1844ea7fb444fbe97d3bb3bce4bf19b"
    private val REDIRECT_URI = "musicmedia://callback"
    private val REQUEST_CODE = 1337

    private var spotifyAccessToken: String? = null
    private var spotifyState: SpotifyState? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
                    fetchSpotifyProfileAndTopTracks()
                }
                AuthorizationResponse.Type.ERROR ->
                    Log.e("Spotify", "Auth error: ${response.error}")
                else -> Log.e("Spotify", "Auth canceled")
            }
        }
    }

    private fun isJson(text: String): Boolean {
        return text.trim().startsWith("{") || text.trim().startsWith("[")
    }

    private fun fetchSpotifyProfileAndTopTracks() {
        val token = spotifyAccessToken ?: return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                // ------- PROFILE -------
                val profileRes = client.newCall(
                    Request.Builder()
                        .url("https://api.spotify.com/v1/me")
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                ).execute()

                val profileBody = profileRes.body?.string().orEmpty()

                if (!isJson(profileBody)) {
                    Log.e("Spotify", "Profile response not JSON: $profileBody")
                    return@launch
                }

                val profileJson = JSONObject(profileBody)

                val profile = SpotifyProfile(
                    id = profileJson.optString("id", ""),
                    displayName = profileJson.optString("display_name", "Spotify User"),
                    imageUrl = profileJson.optJSONArray("images")
                        ?.optJSONObject(0)
                        ?.optString("url", null)
                )

                // ------- TOP TRACKS -------
                val tracksRes = client.newCall(
                    Request.Builder()
                        .url("https://api.spotify.com/v1/me/top/tracks?limit=3")
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                ).execute()

                val tracksBody = tracksRes.body?.string().orEmpty()

                if (!isJson(tracksBody)) {
                    Log.e("Spotify", "Tracks response not JSON: $tracksBody")
                    withContext(Dispatchers.Main) {
                        spotifyState = SpotifyState(profile, emptyList())
                    }
                    return@launch
                }

                val tracksJson = JSONObject(tracksBody)
                val items = tracksJson.optJSONArray("items")

                val tracks = mutableListOf<SpotifyTrack>()
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val t = items.getJSONObject(i)

                        val track = SpotifyTrack(
                            id = t.optString("id", ""),
                            name = t.optString("name", ""),
                            artist = t.optJSONArray("artists")
                                ?.optJSONObject(0)
                                ?.optString("name") ?: "",
                            imageUrl = t.optJSONObject("album")
                                ?.optJSONArray("images")
                                ?.optJSONObject(0)
                                ?.optString("url", null)
                        )
                        tracks.add(track)
                    }
                }

                withContext(Dispatchers.Main) {
                    spotifyState = SpotifyState(profile, tracks)

                    val navHost =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val current =
                        navHost.childFragmentManager.primaryNavigationFragment

                    if (current is ProfileFragment) {
                        current.updateSpotifyUi(profile, tracks)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Spotify", "Fetch error: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}