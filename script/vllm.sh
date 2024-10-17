model_path="/openbayes/home/model/CodeLlama-34b-Instruct-hf"
mirror_url="https://hf-mirror.com/"

python -m vllm.entrypoints.openai.api_server \
    --port 8001 \
    --model $model_path \
    --served-model-name CodeLlama \
    --enforce-eager \
    --tensor-parallel-size 2 \
    --gpu-memory-utilization 0.75 \
    --max-model-len 10240 \
    --dtype auto