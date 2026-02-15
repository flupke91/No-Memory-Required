package com.example.myinputmethodservice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends Activity {

    private TextView statusText;
    private static final int REQUEST_REPLACE = 1001;
    private static final int REQUEST_APPEND = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.tv_status);
        updateStatus();

        // 1. 覆盖导入
        findViewById(R.id.btn_replace).setOnClickListener(v -> {
            startFilePicker(REQUEST_REPLACE);
        });

        // 2. 追加导入
        findViewById(R.id.btn_append).setOnClickListener(v -> {
            startFilePicker(REQUEST_APPEND);
        });

        // 3. 清空词库
        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("确认清空")
                    .setMessage("确定要删除所有导入的词条吗？")
                    .setPositiveButton("删除", (d, w) -> clearDictionary())
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void startFilePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            boolean isAppend = (requestCode == REQUEST_APPEND);
            importDictionary(data.getData(), isAppend);
        }
    }

    private void importDictionary(Uri uri, boolean isAppend) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                if (line.contains(",")) {
                    sb.append(line).append("\n");
                    count++;
                }
            }
            reader.close();

            int mode = isAppend ? MODE_APPEND : MODE_PRIVATE;
            OutputStreamWriter writer = new OutputStreamWriter(
                    openFileOutput("user_dict.txt", mode));
            writer.write(sb.toString());
            writer.close();

            String msg = isAppend ? "追加成功！" : "覆盖成功！";
            Toast.makeText(this, msg + " 新增 " + count + " 条", Toast.LENGTH_SHORT).show();
            updateStatus();

        } catch (Exception e) {
            e.printStackTrace();
            statusText.setText("操作失败: " + e.getMessage());
        }
    }

    private void clearDictionary() {
        deleteFile("user_dict.txt");
        Toast.makeText(this, "词库已清空", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void updateStatus() {
        File file = getFileStreamPath("user_dict.txt");
        if (file.exists()) {
            statusText.setText("当前词库大小: " + (file.length() / 1024) + " KB\n(输入法将在下次弹出时自动更新)");
        } else {
            statusText.setText("当前无词库文件");
        }
    }
}