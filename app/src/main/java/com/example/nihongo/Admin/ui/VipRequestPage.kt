package com.example.nihongo.Admin.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nihongo.Admin.viewmodel.AdminVipRequestViewModel
import com.example.nihongo.Admin.viewmodel.VipPaymentRequest
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipRequestPage(
    viewModel: AdminVipRequestViewModel = viewModel(),
    navController: NavController
) {
    Log.d("VipRequestPage", "Composable started")

    val vipRequests by viewModel.vipRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Thêm log để kiểm tra dữ liệu
    LaunchedEffect(Unit) {
        Log.d("VipRequestPage", "LaunchedEffect triggered, reloading data")
        viewModel.loadVipRequests()
    }
    
    // Log khi dữ liệu thay đổi
    LaunchedEffect(vipRequests) {
        Log.d("VipRequestPage", "Received ${vipRequests.size} VIP requests in UI")
        vipRequests.forEach { request ->
            Log.d("VipRequestPage", "Request: $request")
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedRequestId by remember { mutableStateOf("") }
    var rejectReason by remember { mutableStateOf("") }

    // Hiển thị thông báo lỗi/thành công
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    // Để trống title của TopBar
                    Text("")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = NihongoTheme.textDark
                ),
                actions = {
                    // Thêm nút kiểm tra
                    IconButton(onClick = { 
                        viewModel.loadVipRequests()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = NihongoTheme.primaryGreen
                        )
                    }
                },
                navigationIcon = {}
            )
        },
        containerColor = NihongoTheme.backgroundGray,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hiển thị Row với Icon và Text dưới TopBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = NihongoTheme.primaryGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Quản lý yêu cầu VIP",
                    color = NihongoTheme.textDark,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Phần nội dung chính
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = NihongoTheme.primaryGreen
                    )
                } else if (vipRequests.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Không có yêu cầu nâng cấp VIP nào",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vipRequests) { request ->
                            VipRequestItem(
                                request = request,
                                onApprove = { viewModel.approveVipRequest(request.id) },
                                onReject = {
                                    selectedRequestId = request.id
                                    showRejectDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog từ chối yêu cầu
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Từ chối yêu cầu VIP") },
            text = {
                Column {
                    Text("Vui lòng nhập lý do từ chối:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Lý do từ chối") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NihongoTheme.primaryGreen,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectVipRequest(selectedRequestId, rejectReason)
                        showRejectDialog = false
                        rejectReason = ""
                    },
                    enabled = rejectReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NihongoTheme.accentRed,
                        disabledContainerColor = NihongoTheme.accentRed.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Từ chối")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Hủy", color = NihongoTheme.textDark)
                }
            }
        )
    }
}

@Composable
fun VipRequestItem(
    request: VipPaymentRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val requestDate = request.requestDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"

    val statusColor = when(request.status) {
        "approved" -> NihongoTheme.primaryGreen
        "rejected" -> NihongoTheme.accentRed
        else -> Color(0xFFFFC107) // Màu vàng cảnh báo
    }

    val statusText = when(request.status) {
        "approved" -> "Đã duyệt"
        "rejected" -> "Đã từ chối"
        else -> "Đang chờ"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NihongoTheme.secondaryLightGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NihongoTheme.primaryGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = request.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NihongoTheme.textDark
                        )
                        Text(
                            text = request.userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .border(1.dp, statusColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NihongoTheme.backgroundGray)
                    .padding(12.dp)
            ) {
                PaymentDetailRow("Số tiền", "${request.amount} VNĐ")
                PaymentDetailRow("Phương thức", request.paymentMethod)
                PaymentDetailRow("Mã tham chiếu", request.reference)
                PaymentDetailRow("Ngày yêu cầu", requestDate)
            }

            // Actions
            if (request.status == "pending") {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = NihongoTheme.accentRed
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(NihongoTheme.accentRed)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Từ chối")
                        }
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NihongoTheme.primaryGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Phê duyệt")
                        }
                    }
                }
            } else if (request.status == "rejected" && request.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Lý do từ chối:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = NihongoTheme.textDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = request.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PaymentDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = NihongoTheme.textDark
        )
    }
}











