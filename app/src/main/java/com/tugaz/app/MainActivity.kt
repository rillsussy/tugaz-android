package com.tugaz.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF229A0D)
private val SoftGreen = Color(0xFFE9FFD2)
private val Yellow = Color(0xFFFFC400)
private val Ink = Color(0xFF214014)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { TugazApp() } }
}

@Composable
private fun TugazApp() {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(TaskRepository.load(context)) }
    var title by rememberSaveable { mutableStateOf("") }
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var message by rememberSaveable { mutableStateOf("") }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }

    MaterialTheme {
        Surface {
            Scaffold(containerColor = Color.Transparent) { padding ->
                Box(Modifier.fillMaxSize()) {
                    Image(painterResource(R.drawable.tugaz_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        item { Header() }
                        item {
                            AddTaskCard(title, deadline, message, { title = it; message = "" }, { deadline = it; message = "" }) {
                                when {
                                    title.trim().isEmpty() -> message = "Nama tugas belum diisi."
                                    deadline <= System.currentTimeMillis() -> message = "Pilih deadline di masa depan."
                                    else -> {
                                        val task = Task(System.currentTimeMillis(), title.trim(), deadline)
                                        tasks = (tasks + task).sortedBy(Task::deadlineMillis)
                                        TaskRepository.save(context, tasks); DeadlineScheduler.schedule(context, task)
                                        title = ""; deadline = 0L; message = ""
                                    }
                                }
                            }
                        }
                        item { CalendarPanel(tasks) }
                        item { SectionHeading("Daftar Tugas", "!", Icons.Default.TaskAlt) }
                        if (tasks.isEmpty()) item { EmptyState() }
                        else items(tasks, key = Task::id) { task ->
                            TaskCard(task) { DeadlineScheduler.cancel(context, task.id); tasks = tasks.filterNot { it.id == task.id }; TaskRepository.save(context, tasks) }
                        }
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }
}

@Composable private fun Header() = Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("Tugaz", color = Green, fontSize = 42.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.width(12.dp)); Text("Jangan sampai ada tugas yang kelewat!", color = Color(0xFF5E6958), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun AddTaskCard(title: String, deadline: Long, message: String, onTitle: (String) -> Unit, onDeadline: (Long) -> Unit, onSave: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.5.dp, Color(0xFFD8F5B7)), colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading("Tambah Tugas", "+", Icons.Default.TaskAlt)
            OutlinedTextField(title, onTitle, Modifier.fillMaxWidth(), label = { Text("Nama tugas") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Color(0xFFC9EEA8), focusedLabelColor = Green))
            DeadlineButton(deadline, onDeadline)
            if (message.isNotEmpty()) Text(message, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
            Button(onSave, Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59D80C))) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(10.dp)); Text("Simpan tugas", fontSize = 19.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable private fun DeadlineButton(deadline: Long, onSelected: (Long) -> Unit) {
    val context = LocalContext.current
    Button(onClick = {
        val now = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day -> TimePickerDialog(context, { _, hour, minute -> onSelected(Calendar.getInstance().apply { set(year, month, day, hour, minute, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis) }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show() }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftGreen, contentColor = Green), border = BorderStroke(1.dp, Color(0xFFC9EEA8))) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(10.dp)); Text(if (deadline == 0L) "Pilih deadline" else dateText(deadline), fontWeight = FontWeight.Black) }
}

@Composable private fun CalendarPanel(tasks: List<Task>) {
    val month = remember(tasks) { Calendar.getInstance().apply { timeInMillis = tasks.filter { it.deadlineMillis >= System.currentTimeMillis() }.minByOrNull(Task::deadlineMillis)?.deadlineMillis ?: System.currentTimeMillis(); set(Calendar.DAY_OF_MONTH, 1) } }
    val title = remember { SimpleDateFormat("MMMM yyyy", Locale("id", "ID")) }.format(month.time)
    val first = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val last = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = List(first) { 0 } + (1..last).toList()
    val deadlines = tasks.mapNotNull { task ->
        Calendar.getInstance().apply { timeInMillis = task.deadlineMillis }.takeIf {
            it.get(Calendar.YEAR) == month.get(Calendar.YEAR) && it.get(Calendar.MONTH) == month.get(Calendar.MONTH)
        }?.get(Calendar.DAY_OF_MONTH)
    }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("Kalender Tugas", "#", Icons.Default.CalendarMonth)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.5.dp, Color(0xFFD8F5B7)), colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Green, fontSize = 22.sp, fontWeight = FontWeight.Black)
                CalendarRow(listOf("S", "S", "R", "K", "J", "S", "M"), emptySet(), true)
                days.chunked(7).forEach { week ->
                    CalendarRow((week + List(7 - week.size) { 0 }).map { day -> if (day == 0) "" else day.toString() }, deadlines, false)
                }
                Text(if (tasks.isEmpty()) "Belum ada deadline tugas." else "Titik hijau menandai deadline tugas.", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable private fun CalendarRow(days: List<String>, deadlines: Set<Int>, header: Boolean) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    days.forEach { label -> val marked = label.toIntOrNull() in deadlines; Box(Modifier.weight(1f).height(if (header) 26.dp else 38.dp).clip(RoundedCornerShape(10.dp)).background(if (marked) SoftGreen else Color.Transparent), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = if (marked) Green else Ink, fontSize = if (header) 13.sp else 15.sp, fontWeight = if (header || marked) FontWeight.Black else FontWeight.SemiBold); if (marked) Box(Modifier.size(5.dp).clip(CircleShape).background(Green)) } } }
}

@Composable private fun SectionHeading(text: String, mark: String, icon: androidx.compose.ui.graphics.vector.ImageVector) = Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clip(CircleShape).background(SoftGreen), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Green) }; Spacer(Modifier.width(12.dp)); Text(text, color = Green, fontSize = 27.sp, fontWeight = FontWeight.Black); Spacer(Modifier.width(5.dp)); Text(mark, color = Yellow, fontSize = 27.sp, fontWeight = FontWeight.Black) }

@Composable private fun EmptyState() = Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0xFFD8F5B7)), colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF))) { Text("Belum ada tugas. Tambah tugas pertama kamu di atas.", Modifier.padding(18.dp), color = Ink, fontWeight = FontWeight.SemiBold) }

@Composable private fun TaskCard(task: Task, delete: () -> Unit) {
    val urgent = task.deadlineMillis - System.currentTimeMillis() <= 24 * 60 * 60 * 1000L
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.5.dp, if (urgent) Yellow else Color(0xFFC9EEA8)), colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF))) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(54.dp).clip(CircleShape).background(if (urgent) Color(0xFFFFF4CF) else SoftGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.TaskAlt, null, tint = if (urgent) Color(0xFFFFA800) else Green) }; Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(task.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(dateText(task.deadlineMillis), color = if (urgent) Color(0xFFE53935) else Green, fontWeight = FontWeight.Bold) }; IconButton(delete) { Icon(Icons.Default.Delete, "Hapus tugas", tint = Color(0xFF777777)) } } }
}

private fun dateText(millis: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
