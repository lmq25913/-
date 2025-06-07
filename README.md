# Excel文件导入数据库

## 项目描述
本项目是一个自动化工具，用于将指定目录下的Excel文件数据批量导入MySQL数据库。支持处理 `.xlsx` 和 `.xls` 格式的Excel文件，并能自动将其他格式的Excel文件转换为 `.xlsx` 格式。该工具特别适合需要批量处理Excel数据并存储到数据库的场景。

## 主要功能
- 自动扫描指定目录下的所有Excel文件
- 支持多种Excel格式（.xlsx, .xls）的处理
- 自动转换非.xlsx格式的Excel文件
- 支持多工作表（Sheet）导入
- 自动创建数据库和数据表
- 智能主键处理：
  - 自动识别序列列（包含"序列"、"序号"、"编号"、"ID"等关键词的列）
  - 对无序列列的表自动添加自增ID主键
- 支持数据导入模式配置（替换/追加）
- 批量数据处理，提高导入效率
- 详细的导入过程日志输出

## 环境要求
- Python 3.8+
- MySQL 5.7+
- Windows操作系统（需要支持 pywin32）

## 依赖包
```
et_xmlfile==2.0.0     # openpyxl的XML文件处理依赖
greenlet==3.2.2       # SQLAlchemy异步支持库
numpy==2.2.5          # 数据处理基础库
openpyxl==3.1.5        # 处理.xlsx文件
pandas==2.2.3          # 数据处理核心库
PyMySQL==1.1.1         # MySQL数据库连接器
python-dateutil==2.9.0.post0  # 日期时间处理
pytz==2025.2          # 时区处理
pywin32==310          # Windows COM接口，用于Excel文件转换
six==1.17.0           # Python 2和3兼容性库
SQLAlchemy==2.0.40     # SQL ORM框架
typing_extensions==4.13.2  # Python类型提示扩展
tzdata==2025.2        # 时区数据库
xlrd==2.0.1            # 处理.xls文件
```

## 安装步骤
1. 克隆或下载项目到本地

2. 创建并激活虚拟环境：
   ```bash
   python -m venv venv
   .\venv\Scripts\activate  # Windows
   ```

3. 安装依赖包：
   ```bash
   pip install -r requirements.txt
   ```

## 配置说明
在 `main.py` 中的 `config` 字典配置以下参数：
```python
config = {
    'directory_path': r'C:\Users\limengqi\Desktop\shujui',  # Excel文件存放目录
    'db_connection': 'mysql+pymysql://root:password@localhost:3306/',  # 数据库连接字符串
    'db_name': 'go1',  # 目标数据库名称
    'if_exists': 'replace',  # 表存在时的处理方式：'fail'（报错），'replace'（替换），'append'（追加）
    'chunksize': 1000,  # 批量插入的行数
    'method': 'multi'  # 数据插入方法
}
```

## 使用方法
1. 配置 `config` 中的参数，特别是：
   - `directory_path`：设置Excel文件所在目录
   - `db_connection`：设置数据库连接信息
   - `db_name`：设置目标数据库名称

2. 运行程序：
   ```bash
   python main.py
   ```

## 数据处理流程
1. 扫描指定目录下的所有Excel文件
2. 自动转换非.xlsx格式的文件
3. 创建目标数据库（如不存在）
4. 对每个Excel文件：
   - 读取所有工作表
   - 检查序列列或添加自增ID
   - 创建数据表并设置主键
   - 批量导入数据
5. 输出处理日志

## 注意事项
- 确保MySQL服务已启动
- 确保有足够的数据库权限（创建数据库、创建表、插入数据等）
- 大文件处理时注意内存使用
- 建议先备份重要数据，特别是使用 'replace' 模式时

## 错误处理
程序会处理以下常见错误：
- 目录不存在
- 文件格式不支持
- 数据库连接失败
- 权限不足
- 数据格式错误

所有错误都会在控制台输出详细的错误信息。