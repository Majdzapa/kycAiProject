# Performance Optimization Guide: Local AI

Running Large Language Models (LLMs) like `llama3.2` requires significant processing power. If you see **1000% CPU usage** or slow response times, follow this guide to optimize your environment.

---

## 🚀 The "10x Speed" Fix: Native Ollama (Recommended for Mac/Windows)

The biggest bottleneck is running Ollama *inside* Docker on a Mac. Docker Desktop for Mac runs in a Virtual Machine, which lacks direct access to your Mac's **GPU (Metal)**.

### Step 1: Install Ollama Natively
1.  Download and install Ollama from [ollama.com](https://ollama.com).
2.  Open your terminal and run:
    ```bash
    ollama pull llama3.2
    ollama pull nomic-embed-text
    ```

### Step 2: Configure Docker to use Native Ollama
1.  Stop your Docker containers:
    ```bash
    docker-compose down
    ```
2.  Update your `docker-compose.yml` or your environment variables:
    - Change `OLLAMA_BASE_URL` from `http://ollama:11434` to `http://host.docker.internal:11434`.
3.  Restart your containers:
    ```bash
    docker-compose up -d
    ```

**Result**: Ollama will now use your Mac's **GPU (Apple Silicon Metal)** directly, reducing CPU load from 1000% to ~50% and giving you near-instant responses.

---

## 🛠️ Docker-Only Optimizations

If you *must* run Ollama inside Docker, try these tweaks:

### 1. Limit CPU Usage
Add resource limits to your `docker-compose.yml` to prevent Ollama from freezing your computer:
```yaml
  ollama:
    image: ollama/ollama:latest
    deploy:
      resources:
        limits:
          cpus: '4.0'
          memory: 4096M
```

### 2. Set Threading
You can tell Ollama to use fewer threads to save CPU:
-   **Environment Variable**: `OLLAMA_NUM_PARALLEL=1`
-   **Environment Variable**: `OLLAMA_MAX_LOADED_MODELS=1`

### 3. Use a Smaller Model
If `llama3.2` (3B) is too slow, try these smaller but faster models:
-   `qwen2.5:0.5b` (Extremely fast, low CPU)
-   `tinydolphin` (Very small, good for basic greetings)

Update your `LangChain4jConfig.java` to use the faster model name.

---

## 📉 Why is CPU usage high?
-   **Matrix Multiplication**: LLMs perform billions of math operations per second. Without a GPU, your CPU has to do all this work.
-   **Context Loading**: Every time you send a message, the model processes your entire chat history.
-   **Memory Swapping**: If Docker runs out of RAM, it uses your slow SSD (Swap), which spikes CPU.
