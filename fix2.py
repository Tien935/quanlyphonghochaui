import os
import re

def fix_imports(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Remove the invalid imports
    content = content.replace('import com.example.phonghochaui.CreateBookingActivity;\n', '')
    content = content.replace('import com.example.phonghochaui.MyBookingsActivity;\n', '')
    content = content.replace('import com.example.phonghochaui.NotificationsActivity;\n', '')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

def fix_override(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # The @Override is sitting right above `private View rootView;`
    content = re.sub(r'@Override\s+private View rootView;', 'private View rootView;', content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    base_dir = r"app\src\main\java\com\example\phonghochaui"
    
    fix_imports(os.path.join(base_dir, r"ui\StudentHomeFragment.java"))
    fix_override(os.path.join(base_dir, "CreateBookingFragment.java"))
    fix_override(os.path.join(base_dir, "MyBookingsFragment.java"))
    fix_override(os.path.join(base_dir, "NotificationsFragment.java"))
    
    print("Fixes applied.")

if __name__ == "__main__":
    main()
