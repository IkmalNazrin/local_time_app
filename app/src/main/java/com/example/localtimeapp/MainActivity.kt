package com.example.localtimeapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localtimeapp.ui.theme.LocalTimeAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalTimeAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocalTimeScreen()
                }
            }
        }
    }
}

@Composable
fun LocalTimeScreen() {
    var serverTime by remember { mutableStateOf("Tap to send local time") }
    var isLoading by remember { mutableStateOf(false) }
    var localTimes by remember { mutableStateOf<List<String>>(emptyList()) }
    val currentTime = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://local-time-project.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build())
            .build()
    }

    val apiService = remember { retrofit.create(ApiService::class.java) }

    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
            currentTime.value = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black, Color.DarkGray)
                )
            )
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Local Time App",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Cyan,
            modifier = Modifier.alpha(animatedAlpha)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTime.value,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current Local Time",
                    fontSize = 18.sp,
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Server Status",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = serverTime,
                    fontSize = 20.sp,
                    color = Color.Cyan,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                sdf.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
                                val currentTime = sdf.format(Date())
                                Log.d("LocalTimeApp", "Sending time: $currentTime")
                                val response = apiService.sendLocalTime(TimeRequest(currentTime))
                                serverTime = "Time sent: $currentTime"
                                Log.d("LocalTimeApp", "Response: ${response.message}")
                            } catch (e: Exception) {
                                serverTime = "Error: ${e.message}"
                                Log.e("LocalTimeApp", "Error sending time", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text("Send Local Time", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val times = apiService.getLocalTimes()
                                localTimes = times.map { it.message }
                            } catch (e: Exception) {
                                Log.e("LocalTimeApp", "Error fetching times", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error fetching times: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text("Fetch Local Times", color = Color.Black)
                }
            }
        }

        if (localTimes.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Stored Local Times:",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                localTimes.forEach { time ->
                    Text(
                        text = time,
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Cyan,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
