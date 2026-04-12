import torch

print(f"PyTorch 版本: {torch.__version__}")
print(f"CUDA 是否可用: {torch.cuda.is_available()}")

if torch.cuda.is_available():
    print(f"PyTorch 编译时使用的 CUDA 版本: {torch.version.cuda}")
    print(f"cuDNN 版本: {torch.backends.cudnn.version()}")
    print(f"当前显卡名称: {torch.cuda.get_device_name(0)}")
    print(f"显卡数量: {torch.cuda.device_count()}")
