package com.example.localtimeapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.CertificateException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

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
    var serverStatus by remember { mutableStateOf("Tap to send local time") }
    var isLoading by remember { mutableStateOf(false) }
    val currentTime = remember { mutableStateOf("") }
    val fetchedTimes = remember { mutableStateOf(listOf<TimeResponse>()) }
    var showTable by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://local-time-project.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(getUnsafeOkHttpClient())
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

        if (showTable && fetchedTimes.value.isNotEmpty()) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Server Status",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        IconButton(
                            onClick = { showTable = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Cyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(fetchedTimes.value.size) { index ->
                            val timeResponse = fetchedTimes.value[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "ID: ${timeResponse.id}",
                                    fontSize = 18.sp,
                                    color = Color.Cyan,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Time: ${timeResponse.local_time}",
                                    fontSize = 18.sp,
                                    color = Color.Cyan,
                                    modifier = Modifier.weight(2f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!showTable || fetchedTimes.value.isEmpty()) {
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
                        text = serverStatus,
                        fontSize = 20.sp,
                        color = Color.Cyan,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

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
                        serverStatus = "Time sent: $currentTime"
                        Log.d("LocalTimeApp", "Response: Time sent successfully")
                    } catch (e: Exception) {
                        serverStatus = "Error: ${e.message}"
                        Log.e("LocalTimeApp", "Error sending time: ${e.message}", e)
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
                        val response = apiService.getLocalTimes()
                        fetchedTimes.value = response
                        showTable = true
                        serverStatus = "Fetched ${response.size} times"
                        Log.d("LocalTimeApp", "Response: $serverStatus")
                    } catch (e: Exception) {
                        serverStatus = "Error: ${e.message}"
                        Log.e("LocalTimeApp", "Error fetching times: ${e.message}", e)
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
            Text("Fetch Local Times", color = Color.Black)
        }

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Cyan,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}




fun getUnsafeOkHttpClient(): OkHttpClient {
    val logging = HttpLoggingInterceptor()
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)
    return try {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val sslSocketFactory = sslContext.socketFactory

        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier { _, _ -> true }
        builder.addInterceptor(logging)
        builder.connectTimeout(30, TimeUnit.SECONDS)
        builder.readTimeout(30, TimeUnit.SECONDS)
        builder.writeTimeout(30, TimeUnit.SECONDS)
        builder.build()
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}
