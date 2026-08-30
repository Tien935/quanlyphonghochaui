import os
import re

def refactor_file(filepath, layout_name, class_name, fragment_name):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Imports
    if 'import androidx.fragment.app.Fragment;' not in content:
        content = content.replace('import androidx.appcompat.app.AppCompatActivity;',
                                  'import androidx.appcompat.app.AppCompatActivity;\nimport androidx.fragment.app.Fragment;\nimport androidx.annotation.NonNull;\nimport androidx.annotation.Nullable;\nimport android.view.LayoutInflater;\nimport android.view.ViewGroup;\n')

    # Class signature
    content = content.replace(f'public class {class_name} extends AppCompatActivity', f'public class {fragment_name} extends Fragment')
    content = content.replace(f'public class {class_name}\n        extends AppCompatActivity', f'public class {fragment_name} extends Fragment')
    
    # Constructors / instances
    content = content.replace(f'{class_name}.this', 'requireContext()')
    content = content.replace('(this,', '(requireContext(),')
    content = content.replace('(this)', '(requireContext())')
    content = content.replace('new SessionManager(this)', 'new SessionManager(requireContext())')
    content = content.replace('RetrofitClient.getApiService(this)', 'RetrofitClient.getApiService(requireContext())')
    content = content.replace('new MaterialAlertDialogBuilder(this)', 'new MaterialAlertDialogBuilder(requireContext())')
    
    # EdgeToEdge & setContentView
    content = content.replace('EdgeToEdge.enable(this);', '// EdgeToEdge.enable(requireActivity());')
    content = content.replace(f'setContentView(\n                R.layout.{layout_name}\n        );', '')
    content = content.replace(f'setContentView(R.layout.{layout_name});', '')

    # onCreate -> onCreateView & onViewCreated
    oncreate_pattern = r'protected void onCreate\(\s*Bundle savedInstanceState\s*\)\s*\{'
    replacement = f'''private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {{
        rootView = inflater.inflate(R.layout.{layout_name}, container, false);
        return rootView;
    }}

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {{'''
    
    content = re.sub(oncreate_pattern, replacement, content)
    
    # super.onCreate -> super.onViewCreated
    content = content.replace('super.onCreate(savedInstanceState);', 'super.onViewCreated(view, savedInstanceState);')

    # findViewById -> rootView.findViewById
    content = re.sub(r'(?<!\.)findViewById\(', 'rootView.findViewById(', content)
    
    # finish() -> requireActivity().onBackPressed() or Navigation pop
    content = re.sub(r'\bfinish\(\)', 'requireActivity().onBackPressed()', content)
    
    # Intent this
    content = content.replace('new Intent(this,', 'new Intent(requireContext(),')
    content = content.replace('new Intent(\n                                        this,', 'new Intent(requireContext(),')

    new_filepath = filepath.replace(f'{class_name}.java', f'{fragment_name}.java')
    with open(new_filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"Refactored {class_name} to {fragment_name}")
    # Remove old file
    os.remove(filepath)

def main():
    base_dir = r"app\src\main\java\com\example\phonghochaui"
    
    refactor_file(os.path.join(base_dir, "CreateBookingActivity.java"), "activity_create_booking", "CreateBookingActivity", "CreateBookingFragment")
    refactor_file(os.path.join(base_dir, "MyBookingsActivity.java"), "activity_my_bookings", "MyBookingsActivity", "MyBookingsFragment")
    refactor_file(os.path.join(base_dir, "NotificationsActivity.java"), "activity_notifications", "NotificationsActivity", "NotificationsFragment")

if __name__ == "__main__":
    main()
