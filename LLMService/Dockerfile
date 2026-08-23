FROM python:3.13-slim

WORKDIR /code

# ffmpeg - used by the video/audio flow
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first so Docker can cache dependencies
COPY ./requirements.txt /code/requirements.txt

RUN pip install --no-cache-dir --upgrade pip

# Install project dependencies
RUN pip install --no-cache-dir -r /code/requirements.txt

# Install llama-cpp-python CPU wheel
RUN pip install --no-cache-dir llama-cpp-python \
    --extra-index-url https://abetlen.github.io/llama-cpp-python/whl/cpu

# Directory for the local Qwen model
RUN mkdir -p /app/models

# Download Qwen2.5 0.5B Q4 model during Docker build
RUN python -c "\
from huggingface_hub import hf_hub_download; \
hf_hub_download( \
    repo_id='Qwen/Qwen2.5-0.5B-Instruct-GGUF', \
    filename='qwen2.5-0.5b-instruct-q4_k_m.gguf', \
    local_dir='/app/models' \
)"

# Copy application code
COPY ./app /code/app
COPY ./main.py /code/main.py

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]