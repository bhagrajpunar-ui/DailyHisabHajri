package com.bhagraj.dailyhisabhajri

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF16845B)
private val Red = Color(0xFFD94B4B)
private enum class Page { HOME, INCOME, EXPENSE, ATTENDANCE, HISTORY }
private fun todayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

private data class MoneyRecord(val title: String, val amount: Int, val isIncome: Boolean, val date: String)
private data class AttendanceRecord(val date: String, val status: String)

private fun loadRecords(context: Context): List<MoneyRecord> {
    val raw = context.getSharedPreferences("daily_hisab", Context.MODE_PRIVATE)
        .getString("records", "[]") ?: "[]"
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MoneyRecord(o.getString("title"), o.getInt("amount"), o.getBoolean("income"), o.optString("date", todayKey()))
        }
    } catch (_: Exception) { emptyList() }
}

private fun saveRecords(context: Context, records: List<MoneyRecord>) {
    val arr = JSONArray()
    records.forEach { r ->
        arr.put(JSONObject().put("title", r.title).put("amount", r.amount)
            .put("income", r.isIncome).put("date", r.date))
    }
    context.getSharedPreferences("daily_hisab", Context.MODE_PRIVATE).edit()
        .putString("records", arr.toString()).apply()
}

private fun loadAttendance(context: Context, date: String): String =
    context.getSharedPreferences("daily_hisab", Context.MODE_PRIVATE)
        .getString("attendance_$date", "") ?: ""

private fun saveAttendance(context: Context, date: String, status: String) {
    context.getSharedPreferences("daily_hisab", Context.MODE_PRIVATE).edit()
        .putString("attendance_$date", status).apply()
}

private fun loadAttendanceHistory(context: Context): List<AttendanceRecord> {
    val prefs = context.getSharedPreferences("daily_hisab", Context.MODE_PRIVATE)
    return prefs.all.entries.mapNotNull { (key, value) ->
        if (key.startsWith("attendance_") && value is String && value.isNotBlank()) {
            AttendanceRecord(key.removePrefix("attendance_"), value)
        } else null
    }.sortedByDescending { it.date }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DailyHisabApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHisabApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = remember { todayKey() }
    var page by remember { mutableStateOf(Page.HOME) }
    var records by remember { mutableStateOf(loadRecords(context)) }
    var attendance by remember { mutableStateOf(loadAttendance(context, today)) }
    val todayRecords = records.filter { it.date == today }
    val income = todayRecords.filter { it.isIncome }.sumOf { it.amount }
    val expense = todayRecords.filter { !it.isIncome }.sumOf { it.amount }

    MaterialTheme(colorScheme = lightColorScheme(primary = Green)) {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("Daily Hisab & Hajri", fontWeight = FontWeight.Bold)
                        Text("कमाई • खर्च • हाजिरी", fontSize = 12.sp)
                    }
                })
            },
            bottomBar = {
                NavigationBar {
                    listOf(Page.HOME to "Home", Page.INCOME to "कमाई", Page.EXPENSE to "खर्च",
                        Page.ATTENDANCE to "हाजिरी", Page.HISTORY to "हिस्ट्री").forEach { (p, label) ->
                        NavigationBarItem(
                            selected = page == p,
                            onClick = { page = p },
                            icon = { Icon(Icons.Default.Circle, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            when (page) {
                Page.HOME -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Green),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("आज का बैलेंस", color = Color.White)
                                Text("₹${income - expense}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                                Text("आज की हाजिरी: ${attendance.ifBlank { "नहीं लगी" }}", color = Color.White)
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(Modifier.weight(1f)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("आज की कमाई")
                                    Text("₹$income", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(Modifier.weight(1f)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("आज का खर्च")
                                    Text("₹$expense", color = Red, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item {
                        Text("जल्दी जोड़ें", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { page = Page.INCOME }) { Text("+ कमाई") }
                            Button(onClick = { page = Page.EXPENSE }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("+ खर्च") }
                            OutlinedButton(onClick = { page = Page.ATTENDANCE }) { Text("हाजिरी") }
                        }
                    }
                    item { Text("आज की एंट्री", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    items(todayRecords.reversed()) { RecordRow(it) }
                }

                Page.INCOME -> MoneyEntryForm("कमाई जोड़ें", Green) { title, amount ->
                    val updated = records + MoneyRecord(title, amount, true, today)
                    records = updated
                    saveRecords(context, updated)
                    page = Page.HOME
                }

                Page.EXPENSE -> MoneyEntryForm("खर्च जोड़ें", Red) { title, amount ->
                    val updated = records + MoneyRecord(title, amount, false, today)
                    records = updated
                    saveRecords(context, updated)
                    page = Page.HOME
                }

                Page.ATTENDANCE -> Column(
                    Modifier.fillMaxSize().padding(padding).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("दैनिक हाजिरी", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("तारीख: $today")
                    Text("आज की हाजिरी चुनें")
                    listOf("हाजिर", "गैरहाजिर", "आधी हाजिरी", "छुट्टी").forEach { option ->
                        Card(
                            Modifier.fillMaxWidth().clickable {
                                attendance = option
                                saveAttendance(context, today, option)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (attendance == option) Green.copy(alpha = .12f) else Color.White
                            )
                        ) {
                            Text(
                                if (attendance == option) "✓  $option" else option,
                                Modifier.padding(18.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Page.HISTORY -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text("हिसाब की हिस्ट्री", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                    item { Text("हाजिरी हिस्ट्री", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                    items(loadAttendanceHistory(context)) { entry ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.date, color = Color.Gray)
                                Text(entry.status, color = Green, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item { Text("कमाई और खर्च", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                    items(records.sortedByDescending { it.date }) { RecordRow(it) }
                }
            }
        }
    }
}

@Composable
private fun MoneyEntryForm(title: String, color: Color, onSave: (String, Int) -> Unit) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(description, { description = it }, label = { Text("विवरण") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(amountText, { amountText = it.filter(Char::isDigit) },
            label = { Text("रकम (₹)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(
            onClick = {
                val amount = amountText.toIntOrNull()
                if (description.isNotBlank() && amount != null && amount > 0) onSave(description.trim(), amount)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) { Text("सेव करें") }
        Text("आपका हिसाब इसी मोबाइल में सेव रहेगा।", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun RecordRow(record: MoneyRecord) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(record.title)
                Text(record.date, color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                "${if (record.isIncome) "+" else "-"}₹${record.amount}",
                color = if (record.isIncome) Green else Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
