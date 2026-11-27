package rs.sep.mobilebank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import rs.sep.mobilebank.ui.theme.MobileBankTheme
import java.net.URL
import java.net.HttpURLConnection
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileBankTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScannerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ScannerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bankStatus by remember { mutableStateOf<String?>(null) }
    var scannedText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Mobile Bank")
        Button(onClick = {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener { barcode ->
                    scannedText = barcode.rawValue
                    errorText = null
                }
                .addOnCanceledListener {
                    errorText = "Scanning was cancelled"
                }
                .addOnFailureListener { e ->
                    errorText = "Error: ${e.message}"
                }
        }) {
            Text(text = "Scan QR")
        }
        scannedText?.let { Text(text = it) }
        errorText?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            scope.launch {
                bankStatus = try {
                    withContext(Dispatchers.IO) { checkBank() }
                } catch (e: Exception) {
                    "Bank error: ${e.message}"
                }
            }
        }) {
            Text(text = "Check bank")
        }
        bankStatus?.let { Text(text = it) }
    }
}

private const val BANK_URL = "http://192.168.1.4:8086"

fun checkBank(): String {
    val connection = URL("$BANK_URL/actuator/health").openConnection() as HttpURLConnection
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    try {
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}