FROM python:3.14

WORKDIR /code

COPY ./requirements.txt /code/requirements.txt

RUN mkdir -p /app/models

RUN python -c "\
from huggingface_hub import hf_hub_download; \
hf_hub_download( \
    repo_id='Qwen/Qwen2.5-0.5B-Instruct-GGUF', \
    filename='qwen2.5-0.5b-instruct-q4_k_m.gguf', \
    local_dir='/app/models' \
)"

RUN apt-get update \
    && apt-get install -y ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir --upgrade -r /code/requirements.txt

COPY ./app /code/app
COPY ./main.py /code/main.py

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]