import pymysql
conn = pymysql.connect(
    host="127.0.0.1",
    user="root",
    password="Roo@123",
    database="lms",
    port=3306
)
print("✅ Connection successful")
