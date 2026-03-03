# backend.py
import os
import time
import logging
import traceback

# ==============================
# Gemini API
# ==============================
from google import genai
GEMINI_API_KEY = "AIzaSyCiVYxP_eiKFfrF_lH1l1vqCqUwhL0uzd0"   # <<< THAY API KEY CỦA BẠN
GEMINI_MODEL = "gemini-2.0-flash"

client = genai.Client(api_key=GEMINI_API_KEY)

# ==============================
# Flask + CORS
# ==============================
from flask import Flask, request, jsonify
from flask_cors import CORS

# ==============================
# ChromaDB
# ==============================
import chromadb
from chromadb.config import Settings

# ==============================
# Embedding model (local)
# ==============================
from sentence_transformers import SentenceTransformer

# ==============================
# Config
# ==============================
BACKEND_PORT = 3125
CHROMA_PATH = "./chroma_db"
MODEL_NAME = GEMINI_MODEL     # giữ nguyên biến để code cũ không lỗi

os.makedirs(CHROMA_PATH, exist_ok=True)

MAX_MESSAGE_LENGTH = 1000
HISTORY_LIMIT = 10

# ==============================
# Flask init
# ==============================
app = Flask(__name__)
CORS(app)

logging.basicConfig(
    filename="usage.log",
    level=logging.INFO,
    format="%(asctime)s | %(message)s"
)

# ==============================
# Sessions cache
# ==============================
user_sessions = {}

# ==============================
# ChromaDB init
# ==============================
chroma_client = chromadb.Client(Settings(persist_directory=CHROMA_PATH))
collection = chroma_client.get_or_create_collection(name="nihongo_lessons")

# ==============================
# Embeddings
# ==============================
embedding_model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")

def embed_text(texts):
    if not texts:
        return []
    vectors = embedding_model.encode(texts)
    return [v.tolist() for v in vectors]

def query_docs(query_emb, k=3):
    try:
        result = collection.query(query_embeddings=[query_emb], n_results=k)
        return result.get("documents", [[]])[0]
    except:
        return []

# ==============================
# Gemini Chat Wrapper
# ==============================
def call_gemini(model, messages):
    """
    messages = [
        { "role": "system", "content": "..." },
        { "role": "user", "content": "..." }
    ]
    """
    # Convert messages → 1 string (Gemini không dùng list roles giống OpenAI)
    final_prompt = ""

    for m in messages:
        if m["role"] == "system":
            final_prompt += f"System: {m['content']}\n"
        elif m["role"] == "user":
            final_prompt += f"User: {m['content']}\n"
        else:
            final_prompt += f"Assistant: {m['content']}\n"

    final_prompt += "\nAssistant:"

    response = client.models.generate_content(
        model=model,
        contents=final_prompt
    )

    # Lấy text (Gemini SDK chuẩn)
    return response.text


# ==============================
# Add Knowledge (RAG)
# ==============================
@app.post("/api/add_knowledge")
def add_knowledge():
    try:
        data = request.json or {}
        docs = data.get("docs", [])

        if not docs:
            return jsonify({"msg": "⚠️ Không có dữ liệu để thêm."}), 400

        embeds = embed_text(docs)
        ids = [f"doc_{int(time.time())}_{i}" for i in range(len(docs))]

        collection.add(documents=docs, embeddings=embeds, ids=ids)
        chroma_client.persist()

        return jsonify({"msg": f"✅ Đã thêm {len(docs)} đoạn kiến thức."})

    except Exception as e:
        logging.error(traceback.format_exc())
        return jsonify({"msg": f"⚠️ Lỗi: {str(e)}"}), 500


# ==============================
# CHAT API
# ==============================
@app.post("/api/chat")
def chat():
    start = time.time()

    try:
        data = request.json or {}
        user_message = (data.get("message") or "").strip()
        user_id = (data.get("user_id") or "guest").strip()

        if not user_message:
            return jsonify({"reply": "⚠️ Bạn chưa nhập câu hỏi."})

        if len(user_message) > MAX_MESSAGE_LENGTH:
            return jsonify({"reply": f"⚠️ Tối đa {MAX_MESSAGE_LENGTH} ký tự."})


        # Load history
        history = user_sessions.get(user_id, [])

        # RAG Query
        q_emb = embed_text([user_message])[0]
        docs = query_docs(q_emb)
        context = "\n".join(docs) if docs else ""

        # Build messages
        messages = [
            {
                "role": "system",
                "content": (
                    "Bạn là giáo viên người Việt dạy tiếng Nhật. "
                    "Trả lời dễ hiểu, có ví dụ tiếng Nhật + nghĩa tiếng Việt."
                )
            }
        ]

        messages.extend(history[-HISTORY_LIMIT:])

        if context:
            final_user_prompt = f"Ngữ cảnh:\n{context}\n\nCâu hỏi: {user_message}"
        else:
            final_user_prompt = user_message

        messages.append({"role": "user", "content": final_user_prompt})

        # Gemini API call
        try:
            reply = call_gemini(MODEL_NAME, messages)
        except Exception as e:
            logging.error(traceback.format_exc())
            reply = f"⚠️ AI Error: {str(e)}"

        # Save session
        history.append({"role": "user", "content": user_message})
        history.append({"role": "assistant", "content": reply})
        user_sessions[user_id] = history[-HISTORY_LIMIT:]

        # Auto-learn
        try:
            qa = f"Hỏi: {user_message}\nĐáp: {reply}"
            collection.add(
                documents=[qa],
                embeddings=embed_text([qa]),
                ids=[f"qa_{int(time.time())}"]
            )
            chroma_client.persist()
        except:
            logging.error("auto-learn error")

        elapsed = time.time() - start

        return jsonify({
            "reply": reply,
            "context": context,
            "usage_time": f"{elapsed:.2f}s"
        })

    except Exception as e:
        logging.error(traceback.format_exc())
        return jsonify({"reply": f"⚠️ Lỗi server: {str(e)}"}), 500


# ==============================
# Usage Logs
# ==============================
@app.get("/api/usage")
def get_usage():
    if not os.path.exists("usage.log"):
        return jsonify({"usage_log": []})
    with open("usage.log", "r", encoding="utf-8") as f:
        return jsonify({"usage_log": f.readlines()[-200:]})


# ==============================
# Run app
# ==============================
if __name__ == "__main__":
    print(f"⚡ Backend running at http://0.0.0.0:{BACKEND_PORT}")
    app.run(host="0.0.0.0", port=BACKEND_PORT, debug=True)
