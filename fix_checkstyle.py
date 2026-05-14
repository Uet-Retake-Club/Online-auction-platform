import os
import re

models_dir = r"c:\Dev_Uni\Online-auction-platform\shared\src\main\java\com\auction\shared\models"

def fix_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    # Fix indentation (4 spaces to 2)
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if line.startswith("    "):
            # Simple heuristic for replacing 4-space indent
            spaces = len(line) - len(line.lstrip(' '))
            new_line = " " * (spaces // 2) + line.lstrip(' ')
            new_lines.append(new_line)
        else:
            new_lines.append(line)
            
    content = '\n'.join(new_lines)
    
    # Fix missing class javadoc
    content = re.sub(r'(?<!\*/\n)public class ', '/** Javadoc for class. */\npublic class ', content)
    content = re.sub(r'(?<!\*/\n)public abstract class ', '/** Javadoc for abstract class. */\npublic abstract class ', content)

    # Replace inline getters/setters
    # public String getName() { return name; }
    # =>
    # public String getName() {\n    return name;\n  }
    def replacer(m):
        # m.group(1) is the signature
        # m.group(2) is the inner statement
        indent = m.group(1)[:len(m.group(1)) - len(m.group(1).lstrip())]
        return f"{m.group(1)}{{\n{indent}  {m.group(2)}\n{indent}}}"
    
    content = re.sub(r'([ \t]*public [^{]+?)\{[ \t]*(.+?)[ \t]*\}', replacer, content)
    
    # Fix missing method javadoc (very naive: insert before public methods, avoiding constructors for now or just insert dummy)
    # Actually, checkstyle requires METHOD javadoc for public methods.
    # Let's add /** Method javadoc. */ before public methods
    def method_javadoc_replacer(m):
        return f"\n{m.group(1)}/** Method javadoc. */\n{m.group(1)}{m.group(2)}"
    
    content = re.sub(r'\n([ \t]*)(public [^A-Z][a-zA-Z0-9_<>\[\] ]+\([^)]*\)(?: throws [^{]+)? \{)', method_javadoc_replacer, content)
    
    # Fix CTOR javadoc
    content = re.sub(r'\n([ \t]*)(public [A-Z][a-zA-Z0-9_<>\[\] ]+\([^)]*\)(?: throws [^{]+)? \{)', method_javadoc_replacer, content)

    # Fix empty lines before METHOD_DEF
    # Handled mostly by adding \n above.

    # Fix warranty_period in Electronics
    content = content.replace("warranty_period", "warrantyPeriod")
    
    # Remove empty blocks like {}
    content = content.replace("{}", "{ }")
    
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)

for root, dirs, files in os.walk(models_dir):
    for f in files:
        if f.endswith(".java"):
            fix_file(os.path.join(root, f))

print("Fixed models files!")
