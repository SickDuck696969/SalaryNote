package com.example.tinhluong

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class SalaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).salaryDao()
    private val sharedPreferences = application.getSharedPreferences("Settings", Context.MODE_PRIVATE)

    private val _months = MutableStateFlow<List<WorkMonth>>(emptyList())
    val months: StateFlow<List<WorkMonth>> = _months

    private val _currentDays = MutableStateFlow<List<WorkDay>>(emptyList())
    val currentDays: StateFlow<List<WorkDay>> = _currentDays

    var hourlyWage = MutableStateFlow(sharedPreferences.getInt("hourlyWage", 35))
    val appColor = MutableStateFlow(sharedPreferences.getLong("appColor", 0xFF6650A4))

    val showOvertime = MutableStateFlow(sharedPreferences.getBoolean("showOvertime", false))
    val showSalary = MutableStateFlow(sharedPreferences.getBoolean("showSalary", true))

    // Trạng thái Báo thức
    val isReminderOn = MutableStateFlow(sharedPreferences.getBoolean("isReminderOn", false))
    val reminderHour = MutableStateFlow(sharedPreferences.getInt("reminderHour", 20)) // Mặc định 20:00 (8h tối)
    val reminderMinute = MutableStateFlow(sharedPreferences.getInt("reminderMinute", 0))

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllMonths().collectLatest { monthList ->
                if (monthList.isEmpty()) {
                    val currentMonthStr = "Tháng ${LocalDate.now().monthValue}/${LocalDate.now().year}"
                    dao.insertMonth(WorkMonth(monthName = currentMonthStr))
                } else {
                    _months.value = monthList
                    dao.getDaysForMonth(monthList.last().id).collectLatest { _currentDays.value = it }
                }
            }
        }
    }

    // --- CÀI ĐẶT BÁO THỨC ---
    fun updateReminderTime(hour: Int, minute: Int) {
        reminderHour.value = hour; reminderMinute.value = minute
        sharedPreferences.edit().putInt("reminderHour", hour).putInt("reminderMinute", minute).apply()
        if (isReminderOn.value) scheduleAlarm() // Cập nhật lại giờ nếu đang bật
    }

    fun toggleReminder(isOn: Boolean) {
        isReminderOn.value = isOn
        sharedPreferences.edit().putBoolean("isReminderOn", isOn).apply()
        if (isOn) scheduleAlarm() else cancelAlarm()
    }

    private fun scheduleAlarm() {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(getApplication(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, reminderHour.value)
            set(Calendar.MINUTE, reminderMinute.value)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1) // Nếu giờ đã qua thì đặt sang ngày mai
        }

        // Lặp lại mỗi ngày
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
    }

    private fun cancelAlarm() {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(getApplication(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    // --- XỬ LÝ SETTING KHÁC ---
    fun updateAppColor(colorValue: Long) { appColor.value = colorValue; sharedPreferences.edit().putLong("appColor", colorValue).apply() }
    fun resetTheme() { updateAppColor(0xFF6650A4) }
    fun toggleOvertime(show: Boolean) { showOvertime.value = show; sharedPreferences.edit().putBoolean("showOvertime", show).apply() }
    fun toggleSalary(show: Boolean) { showSalary.value = show; sharedPreferences.edit().putBoolean("showSalary", show).apply() }

    fun updateHourlyWage(newWage: Int) {
        hourlyWage.value = newWage
        sharedPreferences.edit().putInt("hourlyWage", newWage).apply()
        viewModelScope.launch(Dispatchers.IO) {
            _currentDays.value.forEach { day ->
                val newTotal = ((day.hours + day.overtimeHours) * newWage).toInt()
                dao.updateDay(day.copy(totalWage = newTotal))
            }
        }
    }

    fun loadDaysForMonth(monthId: Int) { viewModelScope.launch(Dispatchers.IO) { dao.getDaysForMonth(monthId).collectLatest { _currentDays.value = it } } }

    fun generateNextMonthName(): String {
        var attempt = LocalDate.now()
        while (true) {
            val name = "Tháng ${attempt.monthValue}/${attempt.year}"
            if (_months.value.none { it.monthName == name }) return name
            attempt = attempt.plusMonths(1)
        }
    }

    fun addSingleDay(monthId: Int, shiftType: String, hours: Double, overtime: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = if (shiftType == "Ca đêm") LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi", "VN")))
            else LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi", "VN")))
            val wage = ((hours + overtime) * hourlyWage.value).toInt()
            dao.insertDay(WorkDay(monthId = monthId, dateString = dateStr, shiftType = shiftType, hours = hours, overtimeHours = overtime, totalWage = wage))
        }
    }

    fun addMultiDays(monthId: Int, startMillis: Long, endMillis: Long, shiftType: String, hours: Double, overtime: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.of("UTC")).toLocalDate()
            val end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.of("UTC")).toLocalDate()
            var current = start; val newDaysList = mutableListOf<WorkDay>()
            while (!current.isAfter(end)) {
                val dateStr = current.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi", "VN")))
                val wage = ((hours + overtime) * hourlyWage.value).toInt()
                newDaysList.add(WorkDay(monthId = monthId, dateString = dateStr, shiftType = shiftType, hours = hours, overtimeHours = overtime, totalWage = wage))
                current = current.plusDays(1)
            }
            dao.insertDays(newDaysList)
        }
    }

    fun updateDay(day: WorkDay, newDateStr: String? = null, newShift: String? = null, newHours: Double? = null, newOvertime: Double? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val h = newHours ?: day.hours; val o = newOvertime ?: day.overtimeHours
            val updatedDay = day.copy(dateString = newDateStr ?: day.dateString, shiftType = newShift ?: day.shiftType, hours = h, overtimeHours = o, totalWage = ((h + o) * hourlyWage.value).toInt())
            dao.updateDay(updatedDay)
        }
    }

    fun updateMonthName(month: WorkMonth, newName: String) { viewModelScope.launch(Dispatchers.IO) { dao.updateMonth(month.copy(monthName = newName)) } }
    fun deleteDay(day: WorkDay) { viewModelScope.launch(Dispatchers.IO) { dao.deleteDay(day) } }
    fun deleteMultipleDays(days: List<WorkDay>) { viewModelScope.launch(Dispatchers.IO) { days.forEach { dao.deleteDay(it) } } }
    fun deleteMonth(month: WorkMonth) { viewModelScope.launch(Dispatchers.IO) { dao.deleteMonth(month) } }
    fun createNewMonth(newName: String) { viewModelScope.launch(Dispatchers.IO) { dao.insertMonth(WorkMonth(monthName = newName)) } }

    fun formatMoney(baseAmount: Int): String {
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        return "${formatter.format(baseAmount * 1000)} VND"
    }

    fun convertMillisToDateString(millis: Long): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi", "VN")))
    }
}