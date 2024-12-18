package com.example.onlineexams;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.text.TextUtils; // Import TextUtils for empty string checks
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class Home extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1; // Request code for speech input
    private static final int PERMISSION_REQUEST_CODE = 200; // Permission request code

    private String userUID;
    private String firstName;
    private DatabaseReference database;

    private EditText quizTitleEditText; // Reference for Quiz Title EditText
    private ImageButton speechToTextButton; // Speech-to-text button
    private SpeechRecognizer speechRecognizer; // Speech recognizer instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        database = FirebaseDatabase.getInstance().getReference();
        userUID = getIntent().getStringExtra("User UID"); // Retrieve user UID

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        final TextView name = findViewById(R.id.name);
        final TextView totalQuestions = findViewById(R.id.total_questions);
        final TextView totalPoints = findViewById(R.id.total_points);
        final Button startQuiz = findViewById(R.id.startQuiz);
        final Button createQuiz = findViewById(R.id.createQuiz);
        final RelativeLayout solvedQuizzes = findViewById(R.id.solvedQuizzes);

        quizTitleEditText = findViewById(R.id.quiz_title); // Initialize quiz title EditText
        final EditText startQuizId = findViewById(R.id.start_quiz_id);
        final ImageView signout = findViewById(R.id.signout);

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechToTextButton = findViewById(R.id.speech_to_text); // Initialize speech-to-text button

        // Check for microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }

        speechToTextButton.setOnClickListener(v -> startSpeechRecognition()); // Set click listener for speech button

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot usersRef = snapshot.child("Users").child(userUID);
                firstName = usersRef.child("First Name").getValue(String.class);

                if (usersRef.hasChild("Total Points")) {
                    int points = usersRef.child("Total Points").getValue(Integer.class);
                    totalPoints.setText(String.format("%03d", points)); // Use String.format for padding
                }
                if (usersRef.hasChild("Total Questions")) {
                    int questions = usersRef.child("Total Questions").getValue(Integer.class);
                    totalQuestions.setText(String.format("%03d", questions));
                }
                name.setText("Welcome " + firstName + "!");
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Home.this, "Can't connect", Toast.LENGTH_SHORT).show();
            }
        };

        database.addValueEventListener(listener);

        signout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Home.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        createQuiz.setOnClickListener(v -> {
            String title = quizTitleEditText.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                quizTitleEditText.setError(getString(R.string.error_quiz_title_empty)); // Use string resource for error message
                return;
            }
            Intent intent = new Intent(Home.this, ExamEditor.class);
            intent.putExtra("Quiz Title", title);
            quizTitleEditText.setText("");
            startActivity(intent);
        });

        startQuiz.setOnClickListener(v -> {
            String quizId = startQuizId.getText().toString().trim();
            if (TextUtils.isEmpty(quizId)) {
                startQuizId.setError(getString(R.string.error_quiz_title_empty));
                return;
            }
            Intent intent = new Intent(Home.this, Exam.class);
            intent.putExtra("Quiz ID", quizId);
            startQuizId.setText("");
            startActivity(intent);
        });

        solvedQuizzes.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, ListQuizzes.class);
            intent.putExtra("Operation", "List Solved Quizzes");
            startActivity(intent);
        });


    }

    private void startSpeechRecognition() {
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the Quiz Title");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(Home.this, "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    quizTitleEditText.setText(matches.get(0)); // Set recognized text to EditText
                }
            }

            @Override
            public void onError(int error) {
                Toast.makeText(Home.this, "Speech recognition failed... please try again.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onEvent(int eventType, Bundle params) {}

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onRmsChanged(float rmsdB) {}
        });

        speechRecognizer.startListening(speechIntent); // Start listening for speech input
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (speechRecognizer != null) {
            speechRecognizer.destroy(); // Destroy the recognizer to free resources
        }
    }
}
