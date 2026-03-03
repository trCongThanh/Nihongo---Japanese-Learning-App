package com.example.nihongo.Admin.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class VipPaymentRequest(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val username: String = "",
    val amount: Int = 0,
    val paymentMethod: String = "",
    val reference: String = "",
    val requestDate: Timestamp? = null,
    val status: String = "pending",
    val notes: String = ""
)

class AdminVipRequestViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    // Sử dụng một hằng số cho tên collection để dễ dàng thay đổi nếu cần
    companion object {
        const val VIP_REQUESTS_COLLECTION = "vipRequests"
    }
    
    private val vipRequestsCollection = firestore.collection(VIP_REQUESTS_COLLECTION)
    private val usersCollection = firestore.collection("users")

    // Khởi tạo các StateFlow với giá trị mặc định
    private val _vipRequests = MutableStateFlow<List<VipPaymentRequest>>(emptyList())
    val vipRequests: StateFlow<List<VipPaymentRequest>> = _vipRequests

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Thêm log để kiểm tra
    init {
        Log.d("VipRequestVM", "Initializing with collection: $VIP_REQUESTS_COLLECTION")
        loadVipRequests()
    }

    fun loadVipRequests() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("VipRequestVM", "Starting to load VIP requests from $VIP_REQUESTS_COLLECTION")
                
                val snapshot = vipRequestsCollection
                    .get()
                    .await()
                
                Log.d("VipRequestVM", "Firestore query completed, documents: ${snapshot.documents.size}")
                
                if (snapshot.documents.isEmpty()) {
                    Log.d("VipRequestVM", "No documents found in $VIP_REQUESTS_COLLECTION collection")
                    _vipRequests.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                
                // Xử lý và sắp xếp ở phía client
                val requests = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val data = doc.data
                        if (data == null) {
                            Log.d("VipRequestVM", "Document $id has null data")
                            return@mapNotNull null
                        }
                        
                        // Kiểm tra từng trường và log chi tiết
                        val userId = data["userId"] as? String
                        val userEmail = data["userEmail"] as? String
                        val username = data["username"] as? String
                        val amount = when (val amountValue = data["amount"]) {
                            is Number -> amountValue.toInt()
                            is String -> amountValue.toIntOrNull() ?: 0
                            else -> 0
                        }
                        val paymentMethod = data["paymentMethod"] as? String
                        val reference = data["reference"] as? String
                        val requestDate = data["requestDate"] as? Timestamp
                        val status = data["status"] as? String
                        val notes = data["notes"] as? String
                        
                        Log.d("VipRequestVM", "Parsing document $id: userId=$userId, userEmail=$userEmail, " +
                                "username=$username, amount=$amount, status=$status")
                        
                        VipPaymentRequest(
                            id = id,
                            userId = userId ?: "",
                            userEmail = userEmail ?: "",
                            username = username ?: "",
                            amount = amount,
                            paymentMethod = paymentMethod ?: "",
                            reference = reference ?: "",
                            requestDate = requestDate,
                            status = status ?: "pending",
                            notes = notes ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("VipRequestVM", "Error parsing document ${doc.id}", e)
                        null
                    }
                }
                
                // Sắp xếp ở phía client
                val sortedRequests = requests.sortedByDescending { it.requestDate?.seconds ?: 0 }
                
                _vipRequests.value = sortedRequests
                Log.d("VipRequestVM", "Loaded ${sortedRequests.size} VIP requests")
            } catch (e: Exception) {
                Log.e("VipRequestVM", "Error loading VIP requests", e)
                _errorMessage.value = "Không thể tải yêu cầu VIP: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveVipRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Lấy thông tin yêu cầu
                val requestDoc = vipRequestsCollection.document(requestId).get().await()
                val data = requestDoc.data
                
                if (data != null) {
                    val userId = data["userId"] as? String ?: ""
                    
                    if (userId.isNotEmpty()) {
                        // Cập nhật trạng thái yêu cầu thành "approved"
                        vipRequestsCollection.document(requestId)
                            .update(
                                mapOf(
                                    "status" to "approved",
                                    "approvedDate" to Timestamp.now()
                                )
                            )
                            .await()
                        
                        // Cập nhật trạng thái VIP của người dùng
                        usersCollection.document(userId)
                            .update(
                                mapOf(
                                    "vip" to true,
                                    "vipExpiryDate" to calculateExpiryDate()
                                )
                            )
                            .await()
                        
                        _errorMessage.value = "Đã phê duyệt yêu cầu VIP thành công"
                        loadVipRequests() // Tải lại danh sách
                    } else {
                        _errorMessage.value = "Không tìm thấy ID người dùng"
                    }
                } else {
                    _errorMessage.value = "Không tìm thấy yêu cầu"
                }
            } catch (e: Exception) {
                Log.e("VipRequestVM", "Error approving VIP request", e)
                _errorMessage.value = "Lỗi khi phê duyệt: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectVipRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Cập nhật trạng thái yêu cầu thành "rejected" và thêm lý do
                vipRequestsCollection.document(requestId)
                    .update(
                        mapOf(
                            "status" to "rejected",
                            "notes" to reason,
                            "rejectedDate" to Timestamp.now()
                        )
                    )
                    .await()
                
                _errorMessage.value = "Đã từ chối yêu cầu VIP"
                loadVipRequests() // Tải lại danh sách
            } catch (e: Exception) {
                Log.e("VipRequestVM", "Error rejecting VIP request", e)
                _errorMessage.value = "Lỗi khi từ chối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Tính ngày hết hạn VIP (30 ngày từ ngày hiện tại)
    private fun calculateExpiryDate(): Timestamp {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 30)
        return Timestamp(calendar.time)
    }
}








