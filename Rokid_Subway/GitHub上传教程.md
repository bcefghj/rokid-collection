# GitHub 上传完整教程

## 第一步：安装 Git（如果还没安装）

```bash
# Mac
brew install git

# 或直接在终端输入 git，系统会提示安装 Xcode Command Line Tools
```

## 第二步：创建 GitHub 仓库

1. 打开 https://github.com
2. 点右上角 **"+"** → **"New repository"**
3. 填写信息：
   - **Repository name**: `rokid-subway-navigation`
   - **Description**: `Voice-controlled subway navigation for Rokid AR glasses with offline speech recognition`
   - **Public** (公开，面试官能看到)
   - **不要**勾选 "Add a README"（我们已经有了）
4. 点 **"Create repository"**

## 第三步：上传代码

打开终端，执行以下命令：

```bash
# 1. 进入 GitHub上传 文件夹
cd ~/Desktop/20260316_rokid_system_v2_地铁/2_GitHub上传

# 2. 初始化 Git 仓库
git init

# 3. 添加所有文件
git add .

# 4. 创建第一个提交
git commit -m "feat: Rokid AR glasses subway navigation with offline ASR

- Voice-controlled transit route planning using Sherpa-onnx Zipformer
- Cross-city subway route search via Amap Web API
- Optimized UI for AR glasses HUD (480x640, green theme, touchpad)
- Natural language parsing: supports '从A到B', '去A', 'A到B' patterns"

# 5. 添加 GitHub 远程仓库（把 YOUR_USERNAME 换成你的 GitHub 用户名）
git remote add origin https://github.com/YOUR_USERNAME/rokid-subway-navigation.git

# 6. 推送到 GitHub
git branch -M main
git push -u origin main
```

## 第四步：添加截图（可选但推荐）

```bash
# 在仓库中创建截图文件夹
mkdir -p screenshots

# 把眼镜截图复制进去（如果有的话）
cp ~/Desktop/20260316_rokid_system_v2_地铁/transit_result.png screenshots/
cp ~/Desktop/20260316_rokid_system_v2_地铁/transit_detail.png screenshots/

# 提交截图
git add screenshots/
git commit -m "docs: add AR glasses screenshots"
git push
```

## 第五步：完善仓库（加分项）

### 添加 Topics 标签
在 GitHub 仓库页面，点击 **About** 旁边的齿轮图标，添加 Topics：
- `android` `kotlin` `ar-glasses` `rokid` `speech-recognition`
- `sherpa-onnx` `subway-navigation` `amap` `offline-asr`

### 添加 Release
1. 点击 **Releases** → **Create a new release**
2. Tag: `v2.0.0`
3. Title: `v2.0.0 — Sherpa-onnx ASR + Cross-City Transit`
4. 上传编译好的 APK 文件
5. 发布

## 常见问题

### Q: push 时要输入密码？
GitHub 已不支持密码登录，需要用 Personal Access Token：
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token，勾选 `repo` 权限
3. 复制 token，push 时用 token 代替密码

或者用 SSH：
```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
cat ~/.ssh/id_ed25519.pub  # 复制公钥
# 粘贴到 GitHub → Settings → SSH and GPG keys → New SSH key

# 改用 SSH 地址
git remote set-url origin git@github.com:YOUR_USERNAME/rokid-subway-navigation.git
```

### Q: 文件太大 push 不上去？
确认 `.gitignore` 已排除 `.onnx` 和 `.aar` 文件。如果已经 add 了大文件：
```bash
git rm --cached transit_app/app/libs/sherpa-onnx-1.12.29.aar
git rm --cached transit_app/app/src/main/assets/sherpa/*.onnx
git commit -m "chore: remove large binary files"
```

### Q: 怎么更新代码？
```bash
cd ~/Desktop/20260316_rokid_system_v2_地铁/2_GitHub上传
# 修改代码后
git add .
git commit -m "描述你的修改"
git push
```
