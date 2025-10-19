#!/usr/bin/env python3
import os
import re

def remove_comments_from_file(file_path):
    """Remove comments from Java files"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Remove single-line comments (//)
        content = re.sub(r'^\s*//.*$', '', content, flags=re.MULTILINE)
        
        # Remove multi-line comments (/* ... */)
        content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
        
        # Remove empty lines that might be left after comment removal
        lines = content.split('\n')
        cleaned_lines = []
        for line in lines:
            if line.strip():  # Keep non-empty lines
                cleaned_lines.append(line)
            elif cleaned_lines and cleaned_lines[-1].strip():  # Keep one empty line between sections
                cleaned_lines.append('')
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(cleaned_lines))
        
        print(f"Processed: {file_path}")
        
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

def main():
    # Find all Java files in the iam-service directory
    for root, dirs, files in os.walk('.'):
        for file in files:
            if file.endswith('.java'):
                file_path = os.path.join(root, file)
                remove_comments_from_file(file_path)

if __name__ == "__main__":
    main()

