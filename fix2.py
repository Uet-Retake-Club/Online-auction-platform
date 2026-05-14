import os
import re

models_dir = r"c:\Dev_Uni\Online-auction-platform\shared\src\main\java\com\auction\shared\models"

def fix_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Let's just fix the files manually since they are short
    # I'll just write a script that does regular expressions but more carefully

    # 1. Indentation
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if line.startswith("  ") and not line.startswith("   "):
            # It has 2 spaces. Wait, my previous script changed 4 spaces to 2, and 8 spaces to 4!
            # Let me check what it did:
            # spaces = len(line) - len(line.lstrip(' '))
            # new_line = " " * (spaces // 2) + line.lstrip(' ')
            # If original was 4 spaces, it became 2. If original was 8 spaces, it became 4.
            pass
        
    pass

