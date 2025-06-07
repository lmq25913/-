import pandas as pd
from sqlalchemy import create_engine, text
import pymysql
import os
from pathlib import Path
import win32com.client as win32

# 数据库配置信息
config = {
    'directory_path': r'C:\Users\limengqi\Desktop\shujui',  # Excel文件存放目录
    'db_connection': 'mysql+pymysql://root:l2669906091@localhost:3306/',  # 数据库连接字符串
    'db_name': 'go1',  # 目标数据库名称
    'if_exists': 'replace',  # 表存在时的处理方式：'fail'（报错），'replace'（替换），'append'（追加）
    'chunksize': 1000,  # 批量插入的行数
    'method': 'multi'  # 数据插入方法
}

def convert_to_xlsx(file_path):
    """
    将非.xlsx格式的Excel文件转换为.xlsx格式
    :param file_path: 原始文件路径
    :return: 转换后的文件路径对象
    """
    try:
        excel = win32.gencache.EnsureDispatch('Excel.Application')
        wb = excel.Workbooks.Open(file_path)
        new_file_path = os.path.splitext(file_path)[0] + '.xlsx'
        wb.SaveAs(new_file_path, FileFormat=51)  # 51 代表 .xlsx 格式
        wb.Close()
        excel.Quit()
        return Path(new_file_path)
    except Exception as e:
        print(f"❌ 转换文件 {file_path} 时出错: {str(e)}")
        return None

def import_excel_files_to_mysql():
    """
    主函数：将指定目录下的Excel文件导入MySQL数据库
    """
    try:
        # 1. 创建数据库（如果不存在）
        connection = pymysql.connect(
            host='localhost',
            user='root',
            password='l2669906091'
        )

        with connection.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS {config['db_name']}")
            print(f"数据库 {config['db_name']} 已创建或已存在")
        connection.close()

        # 2. 创建数据库引擎
        engine = create_engine(f"{config['db_connection']}{config['db_name']}")

        # 3. 获取目录下所有Excel文件
        directory = Path(config['directory_path'])
        excel_files = []
        for file_path in directory.iterdir():
            if file_path.is_file():
                if file_path.suffix.lower() in ['.xlsx', '.xls']:
                    excel_files.append(file_path)
                else:
                    converted_path = convert_to_xlsx(str(file_path))
                    if converted_path:
                        excel_files.append(converted_path)

        if not excel_files:
            print(f"错误：在目录 '{config['directory_path']}' 中未找到Excel文件")
            return

        total_files = len(excel_files)
        print(f"找到 {total_files} 个Excel文件")

        # 4. 处理每个Excel文件
        for i, file_path in enumerate(excel_files, 1):
            file_name = file_path.name
            print(f"\n正在处理文件 {i}/{total_files}: {file_name}")

            try:
                excel_file = pd.ExcelFile(file_path)
                sheet_names = excel_file.sheet_names
                print(f"Excel文件中的所有表名: {sheet_names}")

                # 5. 处理每个工作表
                for sheet_name in sheet_names:
                    try:
                        # 生成合法的表名
                        table_name = f"{file_name.split('.')[0]}"
                        table_name = "".join(e for e in table_name if e.isalnum() or e in ['_']).lower()

                        # 读取工作表数据
                        df = excel_file.parse(sheet_name)

                        if df.empty:
                            print(f"  工作表 '{sheet_name}' 为空，跳过")
                            continue

                        # 检查是否存在序列列（查找包含"序列"、"序号"、"编号"等关键词的列）
                        sequence_columns = [col for col in df.columns if any(keyword in str(col) for keyword in ['序列', '序号', '编号', 'id', 'ID'])]
                        
                        if sequence_columns:
                            # 使用找到的第一个序列列作为主键
                            primary_key_column = sequence_columns[0]
                            print(f"  找到序列列: {primary_key_column}，将其设置为主键")
                        else:
                            # 添加自增ID列
                            primary_key_column = 'id'
                            df.insert(0, primary_key_column, range(1, len(df) + 1))
                            print(f"  未找到序列列，添加自增ID列作为主键")

                        print(f"  正在导入工作表 '{sheet_name}' 到表 '{table_name}'...")
                        print(f"  数据基本信息: {df.shape[0]} 行, {df.shape[1]} 列")
                        print(f"  数据前几行预览:\n{df.head().to_string()}")

                        # 将数据导入数据库
                        df.to_sql(
                            name=table_name,
                            con=engine,
                            if_exists=config['if_exists'],
                            index=False,
                            chunksize=config['chunksize'],
                            method=config['method']
                        )

                        # 设置主键
                        with engine.connect() as conn:
                            conn.execute(text(f"ALTER TABLE {table_name} ADD PRIMARY KEY ({primary_key_column})"))
                            conn.commit()

                        print(f"  ✅ 表 '{table_name}' 导入成功，并设置了主键 '{primary_key_column}'")

                    except Exception as e:
                        print(f"  ❌ 处理工作表 '{sheet_name}' 时出错: {str(e)}")

            except Exception as e:
                print(f"❌ 处理文件 '{file_name}' 时出错: {str(e)}")

        print("\n所有Excel文件处理完成！")

    except FileNotFoundError:
        print(f"错误：找不到目录 '{config['directory_path']}'，请检查路径。")
    except Exception as e:
        print(f"发生未知错误: {str(e)}")

if __name__ == "__main__":
    import_excel_files_to_mysql()