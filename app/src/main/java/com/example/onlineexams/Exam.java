package com.example.onlineexams;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Exam extends AppCompatActivity {

    private Question[] data;
    private String quizID;
    private String uid;
    private int oldTotalPoints = 0;
    private int oldTotalQuestions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        quizID = getIntent().getStringExtra("Quiz ID");
        ListView listview = findViewById(R.id.listview);
        Button submit = findViewById(R.id.submit);
        TextView title = findViewById(R.id.title);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("Quizzes").hasChild(quizID)) {
                    DataSnapshot ref = snapshot.child("Quizzes").child(quizID);
                    title.setText(ref.child("Title").getValue(String.class));

                    // Safely get the total number of questions
                    Long numQuestionsLong = ref.child("Total Questions").getValue(Long.class);
                    int num = (numQuestionsLong != null) ? numQuestionsLong.intValue() : 0;
                    data = new Question[num];

                    for (int i = 0; i < num; i++) {
                        if (snapshot.child("Quizzes").child(quizID).child("Questions").child(String.valueOf(i)).exists()) {
                            DataSnapshot qRef = snapshot.child("Quizzes").child(quizID).child("Questions").child(String.valueOf(i));
                            Question question = new Question();
                            question.setQuestion(qRef.child("Question").getValue(String.class));
                            question.setOption1(qRef.child("Option 1").getValue(String.class));
                            question.setOption2(qRef.child("Option 2").getValue(String.class));
                            question.setOption3(qRef.child("Option 3").getValue(String.class));
                            question.setOption4(qRef.child("Option 4").getValue(String.class));

                            // Safely get the correct answer
                            Long ansLong = qRef.child("Ans").getValue(Long.class);
                            int ans = (ansLong != null) ? ansLong.intValue() : 0;
                            question.setCorrectAnswer(ans);
                            data[i] = question;
                        }
                    }

                    ListAdapter listAdapter = new ListAdapter(data);
                    listview.setAdapter(listAdapter);

                    // Safely get old total points and questions
                    Long oldPointsLong = snapshot.child("Users").child(uid).child("Total Points").getValue(Long.class);
                    oldTotalPoints = (oldPointsLong != null) ? oldPointsLong.intValue() : 0;

                    Long oldQuestionsLong = snapshot.child("Users").child(uid).child("Total Questions").getValue(Long.class);
                    oldTotalQuestions = (oldQuestionsLong != null) ? oldQuestionsLong.intValue() : 0;
                } else {
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Exam.this, "Can't connect", Toast.LENGTH_SHORT).show();
            }
        };
        database.addValueEventListener(listener);

        submit.setOnClickListener(v -> {
            DatabaseReference ref = database.child("Quizzes").child(quizID)
                    .child("Answers").child(uid);
            int totalPoints = oldTotalPoints;
            int points = 0;
            for (int i = 0; i < data.length; i++) {
                if (data[i] != null) {
                    ref.child(String.valueOf((i + 1))).setValue(data[i].getSelectedAnswer());
                    if (data[i].getSelectedAnswer() == data[i].getCorrectAnswer()) {
                        totalPoints++;
                        points++;
                    }
                }
            }
            ref.child("Points").setValue(points);
            int totalquestions = oldTotalQuestions + data.length;
            database.child("Users").child(uid).child("Total Points").setValue(totalPoints);
            database.child("Users").child(uid).child("Total Questions").setValue(totalquestions);
            database.child("Users").child(uid).child("Quizzes Solved").child(quizID).setValue("");

            Intent i = new Intent(Exam.this, Result.class);
            i.putExtra("Quiz ID", quizID);
            startActivity(i);
            finish();
        });

    }

    public class ListAdapter extends BaseAdapter {
        Question[] arr;

        ListAdapter(Question[] arr2) {
            arr = arr2;
        }

        @Override
        public int getCount() {
            return arr.length;
        }

        @Override
        public Object getItem(int i) {
            return arr[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            LayoutInflater inflater = getLayoutInflater();
            View v = inflater.inflate(R.layout.question, null);

            TextView question = v.findViewById(R.id.question);
            RadioButton option1 = v.findViewById(R.id.option1);
            RadioButton option2 = v.findViewById(R.id.option2);
            RadioButton option3 = v.findViewById(R.id.option3);
            RadioButton option4 = v.findViewById(R.id.option4);

            question.setText(data[i].getQuestion());
            option1.setText(data[i].getOption1());
            option2.setText(data[i].getOption2());
            option3.setText(data[i].getOption3());
            option4.setText(data[i].getOption4());

            option1.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) data[i].setSelectedAnswer(1);
            });
            option2.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) data[i].setSelectedAnswer(2);
            });
            option3.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) data[i].setSelectedAnswer(3);
            });
            option4.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) data[i].setSelectedAnswer(4);
            });

            switch (data[i].getSelectedAnswer()) {
                case 1:
                    option1.setChecked(true);
                    break;
                case 2:
                    option2.setChecked(true);
                    break;
                case 3:
                    option3.setChecked(true);
                    break;
                case 4:
                    option4.setChecked(true);
                    break;
            }

            return v;
        }
    }
}
