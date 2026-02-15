package com.example.myinputmethodservice;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private StringBuilder composing = new StringBuilder();
    private List<WordEntry> customDictionary = new ArrayList<>();
    private List<String> currentCandidates = new ArrayList<>();
    private long lastLoadTime = 0;

    // 词条数据模型
    private static class WordEntry {
        String english;
        String chinese;
        String pinyin;

        public WordEntry(String english, String chinese, String pinyin) {
            this.english = english;
            this.chinese = chinese;
            this.pinyin = pinyin;
        }
    }
    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private LinearLayout candidateContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        loadDictionary();
    }

    /**
     * 从内部存储读取由 App 导入的词库文件
     */
    private void loadDictionary() {
        customDictionary.clear();
        try {
            File file = getFileStreamPath("user_dict.txt");
            if (!file.exists()) {
                // 如果文件不存在，仅加载默认提示
                customDictionary.add(new WordEntry("ImportFirst", "请先导入", "qingxiandaoru"));
                return;
            }

            // 更新最后加载时间
            lastLoadTime = file.lastModified();

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                // 格式: English,Chinese,Pinyin
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    customDictionary.add(new WordEntry(
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim()
                    ));
                }
            }
            reader.close();
            System.out.println("Dictionary loaded: " + customDictionary.size() + " entries.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateInputView() {
        // 加载包含候选栏和键盘的总布局
        View mRootView = getLayoutInflater().inflate(R.layout.input_method, null);

        // 绑定键盘
        keyboardView = (KeyboardView) mRootView.findViewById(R.id.keyboard);
        keyboard = new Keyboard(this, R.xml.qwerty);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);

        // 绑定候选栏容器
        candidateContainer = mRootView.findViewById(R.id.candidate_container);

        return mRootView;
    }


    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        // 检查文件是否更新
        File file = getFileStreamPath("user_dict.txt");
        if (file.exists() && file.lastModified() > lastLoadTime) {
            loadDictionary(); // 热更新词库
        } else if (customDictionary.isEmpty()) {
            loadDictionary();
        }

        composing.setLength(0);
        currentCandidates.clear();
        setCandidatesViewShown(false);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                if (composing.length() > 0) {
                    composing.deleteCharAt(composing.length() - 1);
                    ic.setComposingText(composing, 1);
                    updateCandidates();
                } else {
                    ic.deleteSurroundingText(1, 0);
                }
                break;
            case Keyboard.KEYCODE_DONE:
            case 10:
                if (composing.length() > 0) {
                    ic.commitText(composing, 1);
                    composing.setLength(0);
                    updateCandidates();
                } else {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                }
                break;
            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code)) {
                    composing.append(code);
                    ic.setComposingText(composing, 1);
                    updateCandidates();
                } else {
                    ic.commitText(String.valueOf(code), 1);
                }
        }
    }

    private void updateCandidates() {
        // 删掉重复的那行头，直接写逻辑
        if (composing.length() > 0) {
            List<String> list = new ArrayList<>();
            String prefix = composing.toString().toLowerCase();
            for (WordEntry entry : customDictionary) {
                if (entry.pinyin.toLowerCase().startsWith(prefix) || entry.english.toLowerCase().startsWith(prefix)) {
                    list.add(entry.chinese);
                }
            }

            if (candidateContainer != null) {
                candidateContainer.removeAllViews(); // 清空旧词
                for (String word : list) {
                    TextView tv = new TextView(this);
                    tv.setText(word);
                    tv.setTextSize(22);
                    tv.setPadding(30, 15, 30, 15);

                    tv.setOnClickListener(v -> {
                        getCurrentInputConnection().commitText(word, 1); // 上屏
                        composing.setLength(0); // 清空拼音缓存
                        updateCandidates();     // 隐藏候选栏
                    });
                    candidateContainer.addView(tv);
                }
            }
        } else {
            if (candidateContainer != null) candidateContainer.removeAllViews();
        }
    }
    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
