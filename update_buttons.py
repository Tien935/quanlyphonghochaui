import os, glob

target = """            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="48dp"
            android:minWidth="0dp"
            android:text="@string/back"
            android:textAllCaps="false"
            android:textColor="@color/white" />"""

replacement = """            style="@style/Widget.Material3.Button.IconButton"
            android:layout_width="48dp"
            android:layout_height="48dp"
            app:icon="@drawable/ic_arrow_back_24"
            app:iconTint="@color/white" />"""

files = glob.glob('d:/PTDTDD/Phonghochaui/app/src/main/res/layout/*.xml')
count = 0
for f in files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    
    content_norm = content.replace('\r\n', '\n')
    if target in content_norm:
        new_content = content_norm.replace(target, replacement)
        with open(f, 'w', encoding='utf-8', newline='\r\n') as file:
            file.write(new_content)
        print("Updated", f)
        count += 1

print(f"Updated {count} files.")
