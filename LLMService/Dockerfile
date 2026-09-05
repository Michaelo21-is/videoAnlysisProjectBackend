FROM python:3.13-slim

WORKDIR /code

# ffmpeg - used by the video/audio flow
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first so Docker can cache dependencies
COPY ./requirements.txt /code/requirements.txt

RUN pip install --no-cache-dir --upgrade pip

# Install the CPU-only torch wheel - the container has no GPU.
# This has to run before the requirements so pip does not pull
# the much larger CUDA build from PyPI.
RUN pip install --no-cache-dir torch \
    --index-url https://download.pytorch.org/whl/cpu

# Install project dependencies
RUN pip install --no-cache-dir -r /code/requirements.txt

# Keep the Hugging Face cache inside the image so the container
# never downloads the model at runtime
ENV HF_HOME=/opt/huggingface

# Download Qwen3-1.7B - the base model of the fine-tuned LoRA adapter
# in app/LocalLLM/qwen-query-generator
RUN python -c "\
from huggingface_hub import snapshot_download; \
snapshot_download( \
    repo_id='Qwen/Qwen3-1.7B', \
    allow_patterns=['*.json', '*.safetensors', '*.txt'] \
)"

# Copy application code
COPY ./app /code/app
COPY ./main.py /code/main.py

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
