# Vector Service 安装指南

## 推荐方式：使用虚拟环境（强烈推荐）

### Windows (PowerShell)

```powershell
# 进入 vector-service 目录
cd vector-service

# 创建虚拟环境
python -m venv venv

# 激活虚拟环境
.\venv\Scripts\Activate.ps1

# 升级 pip
python -m pip install --upgrade pip

# 安装依赖
pip install -r requirements.txt

# 启动服务
python main.py
```

### Linux / macOS

```bash
# 进入 vector-service 目录
cd vector-service

# 创建虚拟环境
python3 -m venv venv

# 激活虚拟环境
source venv/bin/activate

# 升级 pip
pip install --upgrade pip

# 安装依赖
pip install -r requirements.txt

# 启动服务
python main.py
```

## 退出虚拟环境

```bash
# Windows/Linux/macOS
deactivate
```

## 验证安装

安装完成后，检查服务是否正常启动：

```bash
# 健康检查
curl http://localhost:8001/health
```

## 注意事项

1. **为什么使用虚拟环境？**
   - 隔离项目依赖，避免与系统或其他项目的包冲突
   - 易于清理和重建环境
   - 确保依赖版本一致

2. **如果必须使用全局环境**
   - 需要卸载冲突的包（tensorflow、torchaudio、torchvision）
   - 或者调整依赖版本以兼容（不推荐）

3. **首次运行**
   - 首次启动会自动下载 sentence-transformers 模型
   - 模型下载可能需要几分钟时间
   - 模型会缓存到本地，后续启动更快
