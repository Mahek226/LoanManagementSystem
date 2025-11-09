"""
Setup script for Loan Screening Automation System
"""
import os
import sys

def create_env_file():
    """Create .env file from .env.example if it doesn't exist"""
    if not os.path.exists('.env'):
        if os.path.exists('.env.example'):
            with open('.env.example', 'r') as f:
                content = f.read()
            with open('.env', 'w') as f:
                f.write(content)
            print("Created .env file from .env.example")
            print("Please update .env with your database and email credentials")
        else:
            print("Warning: .env.example not found")
    else:
        print(".env file already exists")

def create_upload_dir():
    """Create uploads directory"""
    upload_dir = "uploads"
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)
        print(f"Created {upload_dir} directory")
    else:
        print(f"{upload_dir} directory already exists")

if __name__ == "__main__":
    print("Setting up Loan Screening Automation System...")
    create_env_file()
    create_upload_dir()
    print("\nSetup complete!")
    print("\nNext steps:")
    print("1. Update .env file with your configuration")
    print("2. Install dependencies: pip install -r requirements.txt")
    print("3. Run database migrations: alembic upgrade head")
    print("4. Start the server: uvicorn app.main:app --reload")


