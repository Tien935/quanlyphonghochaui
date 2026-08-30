import os
import re

def fix_imports(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('import com.example.phonghochaui.FindRoomActivity;\n', '')
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

def fix_fragment(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 3. Replace remaining 'this' that refers to Context in fragments
    content = re.sub(r'\bthis,\s*', 'requireContext(),\n', content)
    
    # Fix Intent
    content = content.replace('new Intent(\n                        this', 'new Intent(\n                        requireContext()')
    content = content.replace('new Intent(\n                this', 'new Intent(\n                requireContext()')
    content = content.replace('new Intent(\n                        this,', 'new Intent(\n                        requireContext(),')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    base_dir = r"app\src\main\java\com\example\phonghochaui"
    fix_imports(os.path.join(base_dir, r"ui\StudentHomeFragment.java"))
    fix_fragment(os.path.join(base_dir, "FindRoomFragment.java"))
    print("Fixes applied for FindRoom.")

if __name__ == "__main__":
    main()
