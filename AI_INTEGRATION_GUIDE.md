# Hướng dẫn tích hợp AI vào ứng dụng Nihongo

## Tổng quan

Ứng dụng Nihongo đã được tích hợp các tính năng AI thông minh để hỗ trợ người dùng học tiếng Nhật hiệu quả hơn. Các tính năng AI được thiết kế theo mô hình Context-Aware AI, không phải chatbot truyền thống.

## Các tính năng AI đã được tích hợp

### 1. Sensei Overlay (Lớp phủ thông minh)
**Vị trí**: Màn hình bài tập (ExerciseScreen, QuizScreen)

**Cách hoạt động**:
- Người dùng nhấn giữ nút Micro ở góc màn hình
- Hỏi câu hỏi về ngữ pháp (ví dụ: "Tại sao câu này dùng 'ni' mà không phải 'de'?")
- AI sẽ:
  - Vẽ vòng tròn sáng (highlight) quanh phần ngữ pháp được hỏi
  - Phát âm thanh giải thích
  - Hiển thị subtitle trong khung bubble ngay bên dưới vùng được highlight

**Component**: `AISenseiOverlay.kt`

**Sử dụng**:
```kotlin
SenseiOverlay(
    isVisible = showSenseiOverlay,
    highlightRegion = currentHighlight,
    subtitle = aiExplanation,
    isListening = isRecording,
    onClose = { showSenseiOverlay = false },
    onStartListening = { /* Start recording */ },
    onStopListening = { /* Stop recording and process */ }
)
```

### 2. Interactive Floating Sensei (Sensei Trôi Nổi)
**Vị trí**: Tất cả màn hình học tập

**Cách hoạt động**:
- Avatar nhỏ trôi nổi ở góc màn hình
- Tự động xuất hiện khi:
  - Người dùng làm sai 2 lần liên tiếp
  - Dừng lại quá lâu ở một câu hỏi
- Avatar thay đổi biểu cảm và hiện bong bóng chat gợi ý

**Component**: `FloatingSensei` trong `AISenseiOverlay.kt`

**Sử dụng**:
```kotlin
FloatingSensei(
    isVisible = showFloatingSensei,
    message = "Cấu trúc này là V-te imasu đó, bạn cần mình nhắc lại không?",
    expression = SenseiExpression.Thinking,
    onDismiss = { showFloatingSensei = false }
)
```

### 3. Tap-to-Explain (Chạm để giảng)
**Vị trí**: Màn hình Đọc hiểu, Flashcard

**Cách hoạt động**:
- Người dùng chạm vào một từ vựng hoặc cấu trúc ngữ pháp
- Bottom Sheet trượt lên từ dưới (chiếm 1/3 màn hình)
- Hiển thị giải thích chi tiết trong ngữ cảnh câu hiện tại

**Component**: `TapToExplainBottomSheet.kt`

**Sử dụng**:
```kotlin
TapToExplainBottomSheet(
    isVisible = showExplanation,
    word = selectedWord,
    explanation = aiExplanation.explanation,
    context = currentSentence,
    onDismiss = { showExplanation = false }
)
```

## Backend AI Service

### AIService.kt
Service để giao tiếp với backend AI server.

**Endpoints cần implement**:
- `POST /api/explain-grammar`: Giải thích ngữ pháp
- `POST /api/explain-word`: Giải thích từ vựng
- `POST /api/text-to-speech`: Chuyển text thành voice
- `POST /api/speech-to-text`: Chuyển voice thành text

**Request format**:
```json
{
  "grammar_point": "ni vs de",
  "context": "Câu hỏi đầy đủ",
  "question": "Tại sao dùng ni?"
}
```

**Response format**:
```json
{
  "explanation": "Ở đây dùng 'ni' để chỉ thời điểm cụ thể...",
  "highlight_text": "ni",
  "examples": ["Ví dụ 1", "Ví dụ 2"],
  "voice_url": "https://..."
}
```

## Cách tích hợp vào màn hình

### QuizScreen - Tích hợp Sensei Overlay

```kotlin
@Composable
fun QuizScreen(...) {
    var showSenseiOverlay by remember { mutableStateOf(false) }
    var highlightRegion by remember { mutableStateOf<HighlightRegion?>(null) }
    var aiExplanation by remember { mutableStateOf("") }
    val aiService = remember { AIService() }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Your existing quiz content
        
        // Add Sensei Overlay
        SenseiOverlay(
            isVisible = showSenseiOverlay,
            highlightRegion = highlightRegion,
            subtitle = aiExplanation,
            isListening = isRecording,
            onClose = { showSenseiOverlay = false },
            onStartListening = {
                // Start voice recording
                isRecording = true
            },
            onStopListening = {
                isRecording = false
                // Process voice and get explanation
                coroutineScope.launch {
                    val question = aiService.speechToText(audioData)
                    val explanation = aiService.explainGrammar(
                        grammarPoint = extractGrammarPoint(question),
                        context = currentExercise.question ?: "",
                        question = question
                    )
                    aiExplanation = explanation.explanation
                    highlightRegion = findHighlightRegion(explanation.highlightText)
                }
            }
        )
    }
}
```

### FlashcardScreen - Tích hợp Tap-to-Explain

```kotlin
@Composable
fun FlashcardScreen(...) {
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var explanation by remember { mutableStateOf("") }
    val aiService = remember { AIService() }
    
    // Make text clickable
    Text(
        text = flashcardText,
        modifier = Modifier.clickable {
            // Detect which word was clicked
            selectedWord = detectWordAtPosition(clickPosition)
            if (selectedWord != null) {
                coroutineScope.launch {
                    explanation = aiService.explainWord(
                        word = selectedWord!!,
                        context = flashcardText
                    ).explanation
                    showExplanation = true
                }
            }
        }
    )
    
    // Show explanation bottom sheet
    TapToExplainBottomSheet(
        isVisible = showExplanation,
        word = selectedWord ?: "",
        explanation = explanation,
        context = flashcardText,
        onDismiss = { showExplanation = false }
    )
}
```

## Cấu hình Backend

1. **Cài đặt URL backend** trong `AIService.kt`:
```kotlin
class AIService(
    private val baseUrl: String = "https://your-ai-backend.com/api"
)
```

2. **Implement các endpoints** trên backend Python:
- Sử dụng model AI như GPT-4, Claude, hoặc model tiếng Nhật chuyên biệt
- Tích hợp TTS (Text-to-Speech) cho tiếng Việt
- Tích hợp STT (Speech-to-Text) cho tiếng Việt

3. **Xử lý ngữ cảnh**:
- Backend cần phân tích ngữ cảnh câu hỏi
- Trả về highlight region (tọa độ) để vẽ trên màn hình
- Tạo voice response với giọng nói tự nhiên

## Lưu ý

1. **Performance**: Cache các câu trả lời thường gặp để giảm latency
2. **Offline mode**: Có thể implement cache local cho các câu hỏi phổ biến
3. **Privacy**: Đảm bảo dữ liệu voice được xử lý an toàn
4. **Cost**: Monitor API costs khi sử dụng AI services

## Tương lai

- Conversation Roleplay Mode cho màn hình Luyện nói (Kaiwa)
- Real-time pronunciation feedback
- Adaptive learning paths dựa trên AI analysis

