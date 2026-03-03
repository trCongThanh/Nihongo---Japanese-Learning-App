# Tóm tắt tích hợp AI vào Nihongo App

## Đã hoàn thành

### 1. Backend AI Server (`backend_AI/nihongo.py`)
- ✅ Flask server với CORS enabled
- ✅ Endpoints:
  - `/api/explain-grammar`: Giải thích ngữ pháp trong ngữ cảnh
  - `/api/explain-word`: Giải thích từ vựng trong ngữ cảnh
  - `/api/text-to-speech`: Chuyển text thành speech (Google TTS)
  - `/api/speech-to-text`: Chuyển speech thành text (Google STT)
  - `/health`: Health check endpoint
- ✅ Fallback responses khi không có API keys
- ✅ Response caching để tối ưu performance

### 2. Android App - Network & AI Service
- ✅ `NetworkUtils.kt`: Tự động lấy IP của thiết bị (WiFi hoặc network interface)
- ✅ `AIService.kt`: Cập nhật để tự động sử dụng IP từ NetworkUtils
- ✅ `VoiceRecorder.kt`: Utility để ghi âm và chuyển đổi thành byte array

### 3. UI Components
- ✅ `AISenseiOverlay.kt`: Overlay hiển thị giải thích với highlight regions
- ✅ `TapToExplain.kt`: Bottom sheet hiển thị giải thích khi tap vào từ
- ✅ `FloatingSensei.kt`: Avatar trôi nổi thông minh với các biểu cảm

### 4. Tích hợp vào màn hình

#### QuizScreen
- ✅ Sensei Overlay: Hiển thị khi user nhấn giữ mic và hỏi về ngữ pháp
- ✅ Floating Sensei: Xuất hiện khi user sai 2 lần liên tiếp
- ✅ Voice recording và speech-to-text integration
- ✅ AI explanation với highlight regions

#### FlashcardScreen
- ✅ Tap-to-Explain: Tap vào từ vựng để xem giải thích chi tiết
- ✅ Bottom sheet với loading state
- ✅ Tự động gọi AI service để lấy explanation

### 5. Permissions
- ✅ `RECORD_AUDIO`: Đã thêm vào AndroidManifest.xml

## Cách chạy Backend

1. Cài đặt dependencies:
```bash
cd backend_AI
pip install -r requirements.txt
```

2. (Tùy chọn) Cấu hình API keys:
```bash
export OPENAI_API_KEY="your-openai-key"
export GOOGLE_TTS_API_KEY="your-google-tts-key"
export GOOGLE_STT_API_KEY="your-google-stt-key"
```

3. Chạy server:
```bash
python nihongo.py
```

Server sẽ chạy trên `http://0.0.0.0:5000` và app Android sẽ tự động kết nối đến IP của máy.

## Lưu ý

1. **IP Address**: App tự động lấy IP của thiết bị. Đảm bảo Android device và máy chạy backend cùng một mạng WiFi.

2. **API Keys**: Backend có thể chạy không cần API keys (sử dụng fallback responses), nhưng để có trải nghiệm tốt nhất nên cấu hình:
   - OpenAI API key cho AI explanations
   - Google TTS/STT API keys cho voice features

3. **Testing**:
   - Test voice recording/playback trên thiết bị thật
   - Test highlight regions trong QuizScreen
   - Test AI responses với và không có API keys

## Các tính năng AI đã tích hợp

1. **Sensei Overlay** (QuizScreen):
   - User nhấn giữ mic và hỏi về ngữ pháp
   - AI highlight vùng được hỏi và phát voice giải thích
   - Hiển thị subtitle karaoke-style

2. **Tap-to-Explain** (FlashcardScreen):
   - User tap vào từ vựng
   - Bottom sheet hiện lên với giải thích chi tiết trong ngữ cảnh

3. **Floating Sensei** (QuizScreen):
   - Xuất hiện khi user sai nhiều lần
   - Đề xuất giúp đỡ một cách chủ động

## Next Steps (Tùy chọn)

1. Thêm Conversation Roleplay Mode cho màn hình luyện nói
2. Cải thiện highlight regions với text bounds thực tế
3. Thêm pronunciation scoring
4. Tối ưu caching và offline support

