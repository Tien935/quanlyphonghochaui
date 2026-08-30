import os
import re

def fix_fragment(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. EdgeToEdge.enable(requireContext()); -> remove or comment out
    content = content.replace('EdgeToEdge.enable(requireContext());', '// EdgeToEdge.enable(requireActivity());')
    
    # 2. Rename lambda 'view' to 'v'
    content = re.sub(r'\(view, insets\) ->', '(v, insets) ->', content)
    content = re.sub(r'\bview ->', 'v ->', content)
    content = re.sub(r'\(parent, view, position, id\) ->', '(parent, v, position, id) ->', content)
    
    # 3. Replace remaining 'this' that refers to Context in fragments
    content = re.sub(r'\bthis,\s*', 'requireContext(),\n', content)
    
    # Fix Toast.makeText
    content = content.replace('Toast.makeText(\n                this', 'Toast.makeText(\n                requireContext()')
    content = content.replace('Toast.makeText(this', 'Toast.makeText(requireContext()')
    
    # Fix Intent
    content = content.replace('new Intent(\n                        this', 'new Intent(\n                        requireContext()')
    content = content.replace('new Intent(\n                this', 'new Intent(\n                requireContext()')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    base_dir = r"app\src\main\java\com\example\phonghochaui"
    
    fix_fragment(os.path.join(base_dir, "CreateBookingFragment.java"))
    fix_fragment(os.path.join(base_dir, "MyBookingsFragment.java"))
    fix_fragment(os.path.join(base_dir, "NotificationsFragment.java"))
    print("Fixes applied.")

if __name__ == "__main__":
    main()
