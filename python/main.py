from datetime import datetime

from fastapi import FastAPI

app = FastAPI(title="Market AI Service")


@app.get("/health")
def health():
    return {"status": "ok", "service": "python-ai"}


@app.get("/ai/ping")
def ai_ping():
    return {
        "status": "ok",
        "time": datetime.now().astimezone().isoformat(),
        "msg": "pong from FastAPI",
    }

if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)