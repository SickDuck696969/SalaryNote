package com.example.tinhluong

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SalaryViewModel) {
    val months by viewModel.months.collectAsState()
    val currentDays by viewModel.currentDays.collectAsState()
    val hourlyWage by viewModel.hourlyWage.collectAsState()
    val appColorLong by viewModel.appColor.collectAsState()

    val showOvertime by viewModel.showOvertime.collectAsState()
    val showSalary by viewModel.showSalary.collectAsState()
    val isReminderOn by viewModel.isReminderOn.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val totalWage = remember(currentDays) { currentDays.sumOf { it.totalWage } }
    val totalHours = remember(currentDays) { currentDays.sumOf { it.hours } }
    val totalOvertime = remember(currentDays) { currentDays.sumOf { it.overtimeHours } }

    if (months.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val appColor = Color(appColorLong.toInt())
    val customColorScheme = MaterialTheme.colorScheme.copy(
        primary = appColor, primaryContainer = appColor.copy(alpha = 0.2f), onPrimaryContainer = appColor
    )

    val pagerState = rememberPagerState(pageCount = { months.size })
    var showAddDayDialog by remember { mutableStateOf(false) }
    var showMultiDayDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEditMonthDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showMonthGridDialog by remember { mutableStateOf(false) }
    var monthGridMode by remember { mutableStateOf("view") }
    var triggerScrollToNewMonth by remember { mutableStateOf(false) }
    var isDeleteMode by remember { mutableStateOf(false) }
    var selectedForDelete by remember { mutableStateOf(setOf<WorkDay>()) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) Toast.makeText(context, "Vui lòng cấp quyền thông báo để app có thể nhắc nhở bạn!", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(pagerState.currentPage) {
        if (months.isNotEmpty()) months.getOrNull(pagerState.currentPage)?.id?.let { viewModel.loadDaysForMonth(it) }
        isDeleteMode = false; selectedForDelete = emptySet()
    }

    LaunchedEffect(months.size) {
        if (triggerScrollToNewMonth && months.isNotEmpty()) { pagerState.animateScrollToPage(months.size - 1); triggerScrollToNewMonth = false }
    }

    MaterialTheme(colorScheme = customColorScheme) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = { Text(months.getOrNull(pagerState.currentPage)?.monthName ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                        actions = {
                            if (!isDeleteMode) IconButton(onClick = { showEditMonthDialog = true }) { Icon(Icons.Default.Edit, "Edit") }
                            IconButton(onClick = { isDeleteMode = !isDeleteMode; selectedForDelete = emptySet() }) {
                                Icon(if (isDeleteMode) Icons.Default.Close else Icons.Default.Delete, "Delete", tint = if (isDeleteMode) Color.Red else LocalContentColor.current)
                            }
                            if (!isDeleteMode) IconButton(onClick = { showSettingsDialog = true }) { Icon(Icons.Default.Settings, "Settings") }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = if (isDeleteMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                    )
                },
                bottomBar = {
                    if (isDeleteMode) {
                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Đã chọn: ${selectedForDelete.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Row {
                                TextButton(onClick = { isDeleteMode = false; selectedForDelete = emptySet() }) { Text("Huỷ", fontSize = 18.sp) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.deleteMultipleDays(selectedForDelete.toList()); isDeleteMode = false; selectedForDelete = emptySet() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Xoá ngay", fontSize = 18.sp) }
                            }
                        }
                    } else {
                        BottomBarControl(
                            totalWage = totalWage, formatMoney = viewModel::formatMoney, showSalary = showSalary,
                            onAddSingleDay = { showAddDayDialog = true }, onAddMultiDays = { showMultiDayDialog = true },
                            onExport = {
                                var text = "BẢNG LƯƠNG ${months[pagerState.currentPage].monthName}\n"; var sum = 0
                                currentDays.forEachIndexed { index, day ->
                                    sum += day.totalWage; text += "${index + 1}. ${day.dateString} | ${day.shiftType} | ${day.hours}h"
                                    if (showOvertime) text += " | TC: ${day.overtimeHours}h"
                                    if (showSalary) text += " -> ${viewModel.formatMoney(day.totalWage)}"
                                    text += "\n"
                                }
                                if (showSalary) text += "-----------------\nTỔNG CỘNG: ${viewModel.formatMoney(sum)}"
                                clipboardManager.setText(AnnotatedString(text)); Toast.makeText(context, "Đã copy văn bản!", Toast.LENGTH_SHORT).show()
                            },
                            onSummaryClick = { showSummaryDialog = true }
                        )
                    }
                }
            ) { paddingValues ->
                HorizontalPager(state = pagerState, modifier = Modifier.padding(paddingValues).fillMaxSize()) { page ->
                    WorkTable(days = currentDays, viewModel = viewModel, formatMoney = viewModel::formatMoney, isDeleteMode = isDeleteMode, selectedForDelete = selectedForDelete, showOvertime = showOvertime, showSalary = showSalary, onToggleSelect = { day -> selectedForDelete = if (selectedForDelete.contains(day)) selectedForDelete - day else selectedForDelete + day })
                }
            }
        }

        if (showSummaryDialog) {
            val dayShifts = remember(currentDays) { currentDays.count { it.shiftType == "Ca ngày" } }
            val nightShifts = remember(currentDays) { currentDays.count { it.shiftType == "Ca đêm" } }
            MonthlySummaryDialog(
                monthName = months[pagerState.currentPage].monthName, totalDays = currentDays.size, dayShifts = dayShifts, nightShifts = nightShifts,
                totalHours = totalHours, totalOvertime = totalOvertime, totalWage = totalWage,
                formatMoney = viewModel::formatMoney, showOvertime = showOvertime, showSalary = showSalary,
                onDismiss = { showSummaryDialog = false },
                onConfirm = { showSummaryDialog = false; triggerScrollToNewMonth = true; viewModel.createNewMonth(viewModel.generateNextMonthName()) },
                onCopy = { textToCopy -> clipboardManager.setText(AnnotatedString(textToCopy)); Toast.makeText(context, "Đã copy tổng kết", Toast.LENGTH_SHORT).show() }
            )
        }

        if (showMonthGridDialog) MonthGridDialog(months = months, mode = monthGridMode, onDismiss = { showMonthGridDialog = false }, onMonthSelected = { index -> if (monthGridMode == "view") { coroutineScope.launch { pagerState.animateScrollToPage(index) }; showMonthGridDialog = false } else viewModel.deleteMonth(months[index]) })
        if (showAddDayDialog) AddSingleDayDialog(showOvertime = showOvertime, onDismiss = { showAddDayDialog = false }, onConfirm = { shift, hours, overtime -> viewModel.addSingleDay(months[pagerState.currentPage].id, shift, hours, overtime); showAddDayDialog = false })
        if (showMultiDayDialog) AddMultiDaysDialog(viewModel = viewModel, showOvertime = showOvertime, onDismiss = { showMultiDayDialog = false }, onConfirm = { start, end, shift, hours, overtime -> viewModel.addMultiDays(months[pagerState.currentPage].id, start, end, shift, hours, overtime); showMultiDayDialog = false })
        if (showEditMonthDialog) EditMonthDialog(currentName = months[pagerState.currentPage].monthName, onDismiss = { showEditMonthDialog = false }, onSave = { newName -> viewModel.updateMonthName(months[pagerState.currentPage], newName); showEditMonthDialog = false })

        if (showSettingsDialog) {
            SettingsDialog(
                currentWage = hourlyWage, currentColor = appColorLong, showOvertime = showOvertime, showSalary = showSalary,
                isReminderOn = isReminderOn, reminderHour = reminderHour, reminderMinute = reminderMinute,
                onDismiss = { showSettingsDialog = false },
                onSaveWage = { newWage -> viewModel.updateHourlyWage(newWage) },
                onToggleOvertime = { viewModel.toggleOvertime(it) }, onToggleSalary = { viewModel.toggleSalary(it) },
                onColorSelected = { colorVal -> viewModel.updateAppColor(colorVal) }, onResetTheme = { viewModel.resetTheme() },
                onViewMonthsClick = { showSettingsDialog = false; monthGridMode = "view"; showMonthGridDialog = true },
                onDeleteMonthsClick = { showSettingsDialog = false; monthGridMode = "delete"; showMonthGridDialog = true },
                onToggleReminder = { isOn ->
                    if (isOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    viewModel.toggleReminder(isOn)
                },
                onPickTime = {
                    TimePickerDialog(context, { _, h, m -> viewModel.updateReminderTime(h, m); viewModel.toggleReminder(true) }, reminderHour, reminderMinute, true).show()
                }
            )
        }
    }
}

// KHỐI COMPONENT CHỌN CA ĐÃ ĐƯỢC ÉP MÀU HIGHLIGHT ĐẬM
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftSelector(selectedShift: String, onShiftSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        FilterChip(
            selected = selectedShift == "Ca ngày",
            onClick = { onShiftSelected("Ca ngày") },
            label = { Text("Ca ngày", fontSize = 16.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary // Ép màu chữ trắng khi được chọn
            )
        )
        FilterChip(
            selected = selectedShift == "Ca đêm",
            onClick = { onShiftSelected("Ca đêm") },
            label = { Text("Ca đêm", fontSize = 16.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
fun TableVerticalDivider() { Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color.Gray)) }

@Composable
fun WorkTable(days: List<WorkDay>, viewModel: SalaryViewModel, formatMoney: (Int) -> String, isDeleteMode: Boolean, selectedForDelete: Set<WorkDay>, showOvertime: Boolean, showSalary: Boolean, onToggleSelect: (WorkDay) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Color.LightGray.copy(alpha = 0.8f))) {
            Text(if (isDeleteMode) "Chọn" else "STT", modifier = Modifier.weight(0.4f).padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            TableVerticalDivider()
            Text("Ngày", modifier = Modifier.weight(1.5f).padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            TableVerticalDivider()
            Text("Loại", modifier = Modifier.weight(1f).padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            TableVerticalDivider()
            Text("Giờ", modifier = Modifier.weight(0.7f).padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            if (showOvertime) { TableVerticalDivider(); Text("T.ca", modifier = Modifier.weight(0.7f).padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center) }
            if (showSalary) { TableVerticalDivider(); Text("Tổng", modifier = Modifier.weight(1.4f).padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center) }
        }
        Divider(color = Color.Gray, thickness = 1.dp)
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(days) { index, day ->
                WorkDayRow(index = index + 1, day = day, viewModel = viewModel, formatMoney = formatMoney, isDeleteMode = isDeleteMode, isSelected = selectedForDelete.contains(day), showOvertime = showOvertime, showSalary = showSalary, onToggleSelect = { onToggleSelect(day) })
                Divider(color = Color.LightGray)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkDayRow(index: Int, day: WorkDay, viewModel: SalaryViewModel, formatMoney: (Int) -> String, isDeleteMode: Boolean, isSelected: Boolean, showOvertime: Boolean, showSalary: Boolean, onToggleSelect: () -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showHoursEdit by remember { mutableStateOf(false) }
    var showOvertimeEdit by remember { mutableStateOf(false) }
    var showShiftEdit by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(if (isSelected) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)).combinedClickable(onClick = { if (isDeleteMode) onToggleSelect() }, onLongClick = {  }), verticalAlignment = Alignment.CenterVertically) {
        if (isDeleteMode) { Box(modifier = Modifier.weight(0.4f).fillMaxHeight(), contentAlignment = Alignment.Center) { Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() }) } }
        else { Text(index.toString(), modifier = Modifier.weight(0.4f).padding(vertical = 12.dp), fontSize = 16.sp, textAlign = TextAlign.Center) }
        TableVerticalDivider()
        Box(modifier = Modifier.weight(1.5f).fillMaxHeight().then(if (!isDeleteMode) Modifier.clickable { showDatePicker = true } else Modifier), contentAlignment = Alignment.Center) { Text(day.dateString, fontSize = 16.sp, color = if (isDeleteMode) Color.Black else MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Medium) }
        TableVerticalDivider()
        Box(modifier = Modifier.weight(1f).fillMaxHeight().then(if (!isDeleteMode) Modifier.clickable { showShiftEdit = true } else Modifier), contentAlignment = Alignment.Center) { Text(day.shiftType, fontSize = 16.sp, color = if (isDeleteMode) Color.Black else MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Medium) }
        TableVerticalDivider()
        Box(modifier = Modifier.weight(0.7f).fillMaxHeight().then(if (!isDeleteMode) Modifier.clickable { showHoursEdit = true } else Modifier), contentAlignment = Alignment.Center) { Text(day.hours.toString(), fontSize = 16.sp, color = if (isDeleteMode) Color.Black else MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Medium) }

        if (showOvertime) { TableVerticalDivider(); Box(modifier = Modifier.weight(0.7f).fillMaxHeight().then(if (!isDeleteMode) Modifier.clickable { showOvertimeEdit = true } else Modifier), contentAlignment = Alignment.Center) { Text(day.overtimeHours.toString(), fontSize = 16.sp, color = if (isDeleteMode) Color.Black else MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Medium) } }
        if (showSalary) { TableVerticalDivider(); Box(modifier = Modifier.weight(1.4f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) { Text(formatMoney(day.totalWage), fontSize = 16.sp, color = Color.Red, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.padding(end = 8.dp)) } }
    }

    if (showDatePicker) { val datePickerState = rememberDatePickerState(); DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { millis -> viewModel.updateDay(day, newDateStr = viewModel.convertMillisToDateString(millis)) }; showDatePicker = false }) { Text("Chọn") } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Huỷ") } }) { DatePicker(state = datePickerState, title = { Text("CHỌN NGÀY", modifier = Modifier.padding(start = 24.dp, top = 24.dp), fontWeight = FontWeight.Bold) }, headline = { Text(datePickerState.selectedDateMillis?.let { viewModel.convertMillisToDateString(it) } ?: "Chưa chọn", modifier = Modifier.padding(start = 24.dp, bottom = 12.dp), fontSize = 24.sp) }, showModeToggle = false) } }
    if (showHoursEdit) { var tempHours by remember { mutableStateOf(day.hours.toString()) }; AlertDialog(onDismissRequest = { showHoursEdit = false }, title = { Text("Sửa số giờ") }, text = { OutlinedTextField(value = tempHours, onValueChange = { tempHours = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }, confirmButton = { Button(onClick = { viewModel.updateDay(day, newHours = tempHours.toDoubleOrNull() ?: day.hours); showHoursEdit = false }) { Text("Lưu") } }) }
    if (showOvertimeEdit) { var tempOt by remember { mutableStateOf(day.overtimeHours.toString()) }; AlertDialog(onDismissRequest = { showOvertimeEdit = false }, title = { Text("Sửa giờ tăng ca") }, text = { OutlinedTextField(value = tempOt, onValueChange = { tempOt = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }, confirmButton = { Button(onClick = { viewModel.updateDay(day, newOvertime = tempOt.toDoubleOrNull() ?: day.overtimeHours); showOvertimeEdit = false }) { Text("Lưu") } }) }
    if (showShiftEdit) {
        var tempShift by remember { mutableStateOf(day.shiftType) }
        AlertDialog(
            onDismissRequest = { showShiftEdit = false }, title = { Text("Đổi loại ca") },
            text = { ShiftSelector(tempShift) { tempShift = it } },
            confirmButton = { Button(onClick = { viewModel.updateDay(day, newShift = tempShift); showShiftEdit = false }) { Text("Lưu") } }
        )
    }
}

@Composable
fun BottomBarControl(totalWage: Int, formatMoney: (Int) -> String, showSalary: Boolean, onAddSingleDay: () -> Unit, onAddMultiDays: () -> Unit, onExport: () -> Unit, onSummaryClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onAddSingleDay, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("1 Ngày", fontSize = 16.sp) }
            Button(onClick = onAddMultiDays, modifier = Modifier.weight(1.3f).padding(horizontal = 4.dp)) { Text("Nhiều Ngày", fontSize = 16.sp) }
            Button(onClick = onExport, modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("Xuất VB", fontSize = 16.sp) }
        }
        Button(onClick = onSummaryClick, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = Modifier.fillMaxWidth().height(70.dp)) {
            if (showSalary) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("TỔNG KẾT THÁNG", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(formatMoney(totalWage), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Yellow) } }
            else { Text("TỔNG KẾT THÁNG", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
fun MonthlySummaryDialog(monthName: String, totalDays: Int, dayShifts: Int, nightShifts: Int, totalHours: Double, totalOvertime: Double, totalWage: Int, formatMoney: (Int) -> String, showOvertime: Boolean, showSalary: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit, onCopy: (String) -> Unit) {
    var copyContent = "TỔNG KẾT BẢNG CÔNG: $monthName\nSố ngày làm: $totalDays ngày\nCa ngày: $dayShifts | Ca đêm: $nightShifts\nTổng số giờ: $totalHours giờ"
    if (showOvertime) copyContent += "\nTổng tăng ca: $totalOvertime giờ"
    if (showSalary) copyContent += "\nTỔNG LƯƠNG: ${formatMoney(totalWage)}"

    AlertDialog(onDismissRequest = onDismiss, title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("TỔNG KẾT", fontSize = 22.sp, fontWeight = FontWeight.Bold); TextButton(onClick = { onCopy(copyContent) }) { Text("📋 Copy", fontSize = 16.sp) } } }, text = { Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Tháng: $monthName", fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(12.dp)); Text("Số ngày làm: $totalDays ngày", fontSize = 18.sp); Text("• Ca ngày: $dayShifts", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, top = 4.dp)); Text("• Ca đêm: $nightShifts", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)); Text("Tổng số giờ: $totalHours giờ", fontSize = 18.sp);
        if (showOvertime) { Text("Tổng tăng ca: $totalOvertime giờ", fontSize = 18.sp) }
        if (showSalary) { Spacer(modifier = Modifier.height(16.dp)); Text("Tổng lương tháng:", fontSize = 18.sp); Text(formatMoney(totalWage), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Red); }
        Spacer(modifier = Modifier.height(16.dp)); Text("Bạn có muốn chốt sổ và tạo bảng cho tháng mới không?", fontStyle = FontStyle.Italic, fontSize = 14.sp)
    } }, confirmButton = { Button(onClick = onConfirm) { Text("Xác nhận tạo", fontSize = 16.sp) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng", fontSize = 16.sp) } })
}

@Composable
fun EditMonthDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Đổi tên tháng") }, text = { OutlinedTextField(value = text, onValueChange = { text = it }) }, confirmButton = { Button(onClick = { onSave(text) }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } })
}

@Composable
fun MonthGridDialog(months: List<WorkMonth>, mode: String, onDismiss: () -> Unit, onMonthSelected: (Int) -> Unit) {
    val isDelete = mode == "delete"
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isDelete) "CHỌN THÁNG ĐỂ XOÁ" else "CHUYỂN ĐẾN THÁNG", fontWeight = FontWeight.Bold, color = if (isDelete) Color.Red else Color.Black) }, text = { LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { itemsIndexed(months) { index, month -> Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(if (isDelete) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer).clickable { onMonthSelected(index) }, contentAlignment = Alignment.Center) { Text(text = month.monthName.replace("Tháng ", "T"), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDelete) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMultiDaysDialog(viewModel: SalaryViewModel, showOvertime: Boolean, onDismiss: () -> Unit, onConfirm: (Long, Long, String, Double, Double) -> Unit) {
    var startMillis by remember { mutableStateOf<Long?>(null) }; var endMillis by remember { mutableStateOf<Long?>(null) }; var selectedShift by remember { mutableStateOf("Ca ngày") }; var hours by remember { mutableStateOf("") }; var overtime by remember { mutableStateOf("") }; var showStartDatePicker by remember { mutableStateOf(false) }; var showEndDatePicker by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ghi công nhiều ngày", fontSize = 20.sp, fontWeight = FontWeight.Bold) }, text = { Column { OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(if (startMillis == null) "Chọn Từ Ngày" else "Từ: ${viewModel.convertMillisToDateString(startMillis!!)}") }; OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(if (endMillis == null) "Chọn Đến Ngày" else "Đến: ${viewModel.convertMillisToDateString(endMillis!!)}") }; Spacer(modifier = Modifier.height(8.dp));
        ShiftSelector(selectedShift) { selectedShift = it }
        Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Số giờ làm chung") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()); if (showOvertime) { Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = overtime, onValueChange = { overtime = it }, label = { Text("Số giờ tăng ca chung") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) } } }, confirmButton = { Button(onClick = { if (startMillis != null && endMillis != null) { val h = hours.toDoubleOrNull() ?: 0.0; val ot = overtime.toDoubleOrNull() ?: 0.0; onConfirm(startMillis!!, endMillis!!, selectedShift, h, ot) } }) { Text("Tạo", fontSize = 18.sp) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ", fontSize = 18.sp) } })
    if (showStartDatePicker) { val datePickerState = rememberDatePickerState(); DatePickerDialog(onDismissRequest = { showStartDatePicker = false }, confirmButton = { TextButton(onClick = { startMillis = datePickerState.selectedDateMillis; showStartDatePicker = false }) { Text("Chọn") } }) { DatePicker(state = datePickerState, title = { Text("TỪ NGÀY", modifier = Modifier.padding(24.dp), fontWeight = FontWeight.Bold) }, headline = { Text(datePickerState.selectedDateMillis?.let { viewModel.convertMillisToDateString(it) } ?: "Chưa chọn", modifier = Modifier.padding(start = 24.dp, bottom = 12.dp), fontSize = 20.sp) }, showModeToggle = false) } }
    if (showEndDatePicker) { val datePickerState = rememberDatePickerState(); DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = { TextButton(onClick = { endMillis = datePickerState.selectedDateMillis; showEndDatePicker = false }) { Text("Chọn") } }) { DatePicker(state = datePickerState, title = { Text("ĐẾN NGÀY", modifier = Modifier.padding(24.dp), fontWeight = FontWeight.Bold) }, headline = { Text(datePickerState.selectedDateMillis?.let { viewModel.convertMillisToDateString(it) } ?: "Chưa chọn", modifier = Modifier.padding(start = 24.dp, bottom = 12.dp), fontSize = 20.sp) }, showModeToggle = false) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSingleDayDialog(showOvertime: Boolean, onDismiss: () -> Unit, onConfirm: (String, Double, Double) -> Unit) {
    var selectedShift by remember { mutableStateOf("Ca ngày") }; var hours by remember { mutableStateOf("") }; var overtime by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ghi công 1 ngày", fontSize = 20.sp, fontWeight = FontWeight.Bold) }, text = { Column {
        ShiftSelector(selectedShift) { selectedShift = it }
        Spacer(modifier = Modifier.height(12.dp)); OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Số giờ làm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()); if (showOvertime) { Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = overtime, onValueChange = { overtime = it }, label = { Text("Giờ tăng ca") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) } } }, confirmButton = { Button(onClick = { val h = hours.toDoubleOrNull() ?: 0.0; val ot = overtime.toDoubleOrNull() ?: 0.0; onConfirm(selectedShift, h, ot) }) { Text("Lưu", fontSize = 18.sp) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ", fontSize = 18.sp) } })
}

@Composable
fun SettingsDialog(
    currentWage: Int, currentColor: Long, showOvertime: Boolean, showSalary: Boolean, isReminderOn: Boolean, reminderHour: Int, reminderMinute: Int,
    onDismiss: () -> Unit, onSaveWage: (Int) -> Unit, onToggleOvertime: (Boolean) -> Unit, onToggleSalary: (Boolean) -> Unit,
    onColorSelected: (Long) -> Unit, onResetTheme: () -> Unit, onViewMonthsClick: () -> Unit, onDeleteMonthsClick: () -> Unit,
    onToggleReminder: (Boolean) -> Unit, onPickTime: () -> Unit
) {
    val themeColors = listOf(0xFF6650A4, 0xFF2196F3, 0xFF4CAF50, 0xFFF29900, 0xFFB3261E)
    var wageStr by remember { mutableStateOf(currentWage.toString()) }
    val timeString = String.format("%02d:%02d", reminderHour, reminderMinute)

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Cài đặt", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (showSalary) { OutlinedTextField(value = wageStr, onValueChange = { wageStr = it }, label = { Text("Tiền lương 1 giờ (VD: 30000)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(16.dp)) }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Hiện cột Tăng ca:", fontSize = 16.sp); Switch(checked = showOvertime, onCheckedChange = onToggleOvertime) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Tính Tiền lương:", fontSize = 16.sp); Switch(checked = showSalary, onCheckedChange = onToggleSalary) }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Nhắc nhở hằng ngày:", fontSize = 16.sp, fontWeight = FontWeight.Bold); Switch(checked = isReminderOn, onCheckedChange = onToggleReminder) }
                if (isReminderOn) { OutlinedButton(onClick = onPickTime, modifier = Modifier.fillMaxWidth()) { Text("Giờ nhắc: $timeString", fontSize = 16.sp) } }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Màu giao diện:", fontWeight = FontWeight.Bold); TextButton(onClick = onResetTheme) { Text("Đặt lại màu") } }
                LazyRow(modifier = Modifier.padding(bottom = 8.dp)) { items(themeColors) { colorValue -> Box(modifier = Modifier.size(44.dp).padding(4.dp).clip(CircleShape).background(Color(colorValue)).border(2.dp, if (currentColor == colorValue) Color.Black else Color.Transparent, CircleShape).clickable { onColorSelected(colorValue) }) } }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onViewMonthsClick, modifier = Modifier.fillMaxWidth()) { Text("Xem danh sách tháng", fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDeleteMonthsClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Xoá tháng", fontSize = 16.sp) }
            }
        },
        confirmButton = { Button(onClick = { onSaveWage(wageStr.toIntOrNull() ?: currentWage); onDismiss() }) { Text("Xong", fontSize = 16.sp) } }
    )
}