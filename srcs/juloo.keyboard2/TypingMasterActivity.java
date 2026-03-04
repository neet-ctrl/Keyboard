package juloo.keyboard2;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TypingMasterActivity extends Activity {

    private TextView tvParagraph;
    private EditText etTypingArea;
    private Button btnNext;

    private String[] paragraphs = {
        "The quick brown fox jumps over the lazy dog. This is a classic pangram that contains every letter of the English alphabet. Typing it repeatedly can help improve your typing speed and accuracy. It's often used for testing typewriters and computer keyboards.",
        "Technology has revolutionized the way we live and work. From smartphones to artificial intelligence, the rapid pace of innovation continues to shape our society. As we embrace these advancements, it's essential to consider their impact on our daily lives.",
        "Learning to code opens up a world of possibilities. It empowers individuals to create software, websites, and applications that solve real-world problems. Whether you're a beginner or an experienced developer, coding is a valuable skill in today's digital age.",
        "Nature offers a profound sense of tranquility and beauty. The rustling of leaves, the chirping of birds, and the gentle flow of a river can soothe the soul. Taking time to connect with nature is essential for our physical and mental well-being.",
        "Reading books is a wonderful way to expand your knowledge and imagination. It transports you to different worlds, introduces you to diverse characters, and allows you to explore complex ideas. Make reading a daily habit to reap its numerous benefits.",
        "Effective communication is key to building strong relationships. It involves not only speaking clearly but also listening attentively. By fostering open and honest dialogue, we can resolve conflicts and foster mutual understanding.",
        "Exercise is vital for maintaining a healthy lifestyle. Whether it's a brisk walk, a rigorous workout, or a yoga session, physical activity benefits both the body and the mind. Incorporate regular exercise into your routine for optimal health.",
        "Traveling allows us to experience new cultures, cuisines, and landscapes. It broadens our horizons and challenges our preconceptions. Embrace the opportunity to explore the world and discover its diverse wonders.",
        "Music has a universal language that transcends boundaries. It can evoke strong emotions, tell compelling stories, and bring people together. Whether you enjoy classical, jazz, or pop, let music be a source of joy and inspiration in your life.",
        "A positive mindset can transform your approach to challenges. By focusing on solutions rather than problems, you can overcome obstacles and achieve your goals. Cultivate a positive attitude to navigate life's ups and downs with resilience."
    };

    private int currentParagraphIndex = 0;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typing_master);

        tvParagraph = findViewById(R.id.tv_paragraph);
        etTypingArea = findViewById(R.id.et_typing_area);
        btnNext = findViewById(R.id.btn_next);

        loadParagraph();

        etTypingArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (etTypingArea.getText().toString().length() == 1) {
                    startTime = System.currentTimeMillis();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(paragraphs[currentParagraphIndex])) {
                    long endTime = System.currentTimeMillis();
                    long timeTaken = endTime - startTime;
                    int words = s.toString().split("\\s+").length;
                    double wpm = (words / (timeTaken / 60000.0));
                    Toast.Utils.makeText(TypingMasterActivity.this, "Completed! Speed: " + Math.round(wpm) + " WPM", Toast.LENGTH_LONG).show();
                    btnNext.setEnabled(true);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            currentParagraphIndex = (currentParagraphIndex + 1) % paragraphs.length;
            loadParagraph();
            etTypingArea.setText("");
            btnNext.setEnabled(false);
        });
    }

    private void loadParagraph() {
        tvParagraph.setText(paragraphs[currentParagraphIndex]);
    }
}
// Added file to manifest
// Updated Activity logic correctly
