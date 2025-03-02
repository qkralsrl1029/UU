package com.example.uu;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.BaseInterpolator;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.owl93.dpb.CircularProgressView;
import com.owl93.dpb.DeterminateProgressViewListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class fragment_record_results extends Fragment {

    // db init
    DatabaseHelper dbHelper;
    SQLiteDatabase sqLiteDb;

    //firebase init
    DatabaseReference databaseReference;
    FirebaseDatabase database;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser firebaseUser ;

    //data list
    ArrayList<BarEntry> barEntries=new ArrayList<>();
    ArrayList<String> xAxisName= new ArrayList<>();

    //for circular progress bar
    private float progress;
    private String achievementName;
    CircularProgressView personalAchievement;
    TextView achievementText;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    int total_goal;
    int current_state;

    private ArrayList<RecentData>recentData=new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup rootView=(ViewGroup) inflater.inflate(R.layout.fragment_record_results, container, false);

        //init local db
        dbHelper=new DatabaseHelper(getContext());
        sqLiteDb=dbHelper.getReadableDatabase();
        preferences= getActivity().getSharedPreferences("achievement", Activity.MODE_PRIVATE);
        editor=preferences.edit();

        //init firebase db
        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = mFirebaseAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("UU");

        //CircularProgressView
        personalAchievement=(CircularProgressView) rootView.findViewById(R.id.personalAchievement);
        achievementText=rootView.findViewById(R.id.achievementText);

        //기존 설정했던 업적 보여주기
        setAchievement();

        //나만의 업적 재설정
        personalAchievement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetAchievement();
            }
        });


        //HorizontalBarChart
        //detail --> https://weeklycoding.com/mpandroidchart-documentation/getting-started/
        HorizontalBarChart mBarchart=(HorizontalBarChart)rootView.findViewById(R.id.id_horizontal_barchart);
        //prepare data
        getRecordData();

        barchart(mBarchart,barEntries,xAxisName);
        mBarchart.animateY(2000);

        //testkang
        for(int i=0;i<10;i++){
            recentData.add(new RecentData("https://firebasestorage.googleapis.com/v0/b/doubleu-2df72.appspot.com/o/recruitment%2FX0632jt7bMTs72MlsT2M.png?alt=media","12.01","종로구","3.4","34"));
        }

        PersonalRecentDataAdapter personalRecentDataAdapter=new PersonalRecentDataAdapter(recentData);
        RecyclerView recyclerView=(RecyclerView) rootView.findViewById(R.id.mypersonalRecord);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(personalRecentDataAdapter);

        return rootView;
    }

    public static void barchart(BarChart barChart, ArrayList<BarEntry> arrayList, final ArrayList<String> xAxisValues) {
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);
        barChart.setDrawValueAboveBar(true);
        barChart.setMaxVisibleValueCount(25);
        barChart.setPinchZoom(false);

        barChart.setDrawGridBackground(true);
        BarDataSet barDataSet = new BarDataSet(arrayList, "M(미터)");
        barDataSet.setColors(0XFF00BFFF);
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.9f);
        barData.setValueTextSize(0f);

        barChart.setBackgroundColor(Color.TRANSPARENT); //set whatever color you prefer
        barChart.setDrawGridBackground(false);

        Legend l = barChart.getLegend(); // Customize the legends
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setTextSize(10f);
        l.setFormSize(10f);

//To set components of x axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextSize(13f);
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisValues));
        xAxis.setDrawGridLines(false);

        barChart.setFitBars(true);

        barChart.setData(barData);
        barChart.invalidate();//refresh

    }

    private void getRecordData() {
        Cursor cursor;

        String day;
        int distance;

        xAxisName.add("temp");

        // Query
        for(int i=-6;i<1;i++){
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE , i);
            String targetDate = new java.text.SimpleDateFormat("yyyy:MM:dd%").format(calendar.getTime());
            String queryDistanceSum="SELECT SUM("+DatabaseHelper.RUNNING_DISTANCE+") FROM "+DatabaseHelper.TABLE_NAME+" WHERE "+DatabaseHelper.PRIMARY_KEY+" LIKE '"+targetDate+"';";

            cursor = sqLiteDb.rawQuery(queryDistanceSum,null);
            cursor.moveToFirst();
            distance=cursor.getInt(0);
            day=getDateDay(calendar.get(Calendar.DAY_OF_WEEK));

            barEntries.add(new BarEntry(i+7,distance+5));
            xAxisName.add(day);
        }
    }

    private static String getDateDay(int dayNum)  {
        String day;

        switch (dayNum) {
            case 1:
                day = "일";
                break;
            case 2:
                day = "월";
                break;
            case 3:
                day = "화";
                break;
            case 4:
                day = "수";
                break;
            case 5:
                day = "목";
                break;
            case 6:
                day = "금";
                break;
            case 7:
                day = "토";
                break;
            default:
                day=null;

        }

        return day;
    }

    private void setAchievement(){      //이미 저장해둔 지표가 있으면 가져와서 반영

        if((preferences!=null)&&preferences.contains("goalObj")){
            int goalIndex=preferences.getInt("goalObj",0);
            int goal=preferences.getInt("goal",0);

            Log.d("check",goalIndex+"  "+goal);
            updateCircularView(goalIndex,goal);
        }
    }

    private void resetAchievement(){    //사용자가 개인 목표를 다시 설정할경우, 그에 맞춰 최신화
        Dialog personalDialog=new Dialog(getActivity());
        personalDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        personalDialog.setContentView(R.layout.dialog_personal_achievement);
        personalDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        personalDialog.show();

        TextView ach0=personalDialog.findViewById(R.id.achievement0);
        ach0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { drawDialog(0); personalDialog.dismiss(); }});

        TextView ach1=personalDialog.findViewById(R.id.achievement1);
        ach1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { drawDialog(1); personalDialog.dismiss(); }});

        TextView ach2=personalDialog.findViewById(R.id.achievement2);
        ach2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { drawDialog(2); personalDialog.dismiss(); }});

        TextView ach3=personalDialog.findViewById(R.id.achievement3);
        ach3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { drawDialog(3); personalDialog.dismiss(); }});

        TextView button=personalDialog.findViewById(R.id.closePersonalAchievement);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                personalDialog.dismiss();
            }
        });

    }

    private void drawDialog(int index){
        Dialog dialog=new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_setachievement);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView title=dialog.findViewById(R.id.title);
        SeekBar setGoal=dialog.findViewById(R.id.setGoal);
        TextView goalMin=dialog.findViewById(R.id.goalMin);
        TextView goalMax=dialog.findViewById(R.id.goalMax);
        Button setBtn=dialog.findViewById(R.id.setBtn);

        switch (index){
            case 0:
                title.setText("얼만큼 달리고 싶으세요?");
                setGoal.setProgress(0);
                setGoal.setMax(9);
                goalMin.setText("1km");
                goalMax.setText("10km");
                break;
            case 1:
                title.setText("얼마동안 운동 해볼까요?");
                setGoal.setProgress(0);
                setGoal.setMax(7);
                goalMin.setText("30min");
                goalMax.setText("240min");
                break;
            case 2:
                title.setText("몇번의 모집에 참가해볼까요?");
                setGoal.setProgress(0);
                setGoal.setMax(4);
                goalMin.setText("1");
                goalMax.setText("5");
                break;
            default:
                editor.clear();
                editor.putInt("goalObj",index);
                editor.commit();
                updateCircularView(index,0);
                return;

        }
        dialog.show();

        setBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int userGoal=setGoal.getProgress()+1;
                int goalIndex=index;

                editor.clear();
                editor.putInt("goalObj",goalIndex);
                editor.putInt("goal",userGoal);
                editor.commit();

                updateCircularView(index,userGoal);

                dialog.dismiss();
            }
        });
    }

    private void updateCircularView(int index,int goal){
        String[] objList=new String[] {"Km\n달리기!","분 동안\n운동하기!","회 모집\n참가하기!"};
        Cursor cursor;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE , 0);
        String today = new java.text.SimpleDateFormat("yyyy:MM:dd%").format(calendar.getTime());
        String targetTable="";

        switch (index){
            case 0:
                achievementText.setText(Integer.toString(goal)+objList[index]);
                total_goal=goal*1000;
                targetTable=DatabaseHelper.RUNNING_DISTANCE;
                break;
            case 1:
                achievementText.setText(Integer.toString((goal)*30)+objList[index]);
                total_goal=goal*30;
                targetTable=DatabaseHelper.RUNNING_TIME;
                break;
            case 2:
                achievementText.setText(Integer.toString(goal)+objList[index]);
                total_goal=goal;
                break;
            default:
                achievementText.setText("재충전은\n필수라구요!!Zzz");
                personalAchievement.animateProgressChange(100,2000);
                break;
        }

        if(index==0 || index==1) {
            String queryDistanceSum = "SELECT SUM(" + targetTable + ") FROM " + DatabaseHelper.TABLE_NAME + " WHERE " + DatabaseHelper.PRIMARY_KEY + " LIKE '" + today + "';";
            cursor = sqLiteDb.rawQuery(queryDistanceSum, null);
            cursor.moveToFirst();
            current_state = cursor.getInt(0);
            personalAchievement.animateProgressChange(((float)current_state/total_goal)*100,2000);
        }
        else if(index==2){
            readData(databaseReference.child("UserAccount").child(firebaseUser.getUid()).child("userRecruitJoinNumber"), new fragment_record_achievements.OnGetDataListener() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    current_state = dataSnapshot.getValue(Integer.class);
                }
                @Override
                public void onStart() {

                }

                @Override
                public void onFailure() {

                }
            });
            personalAchievement.animateProgressChange(((float)current_state/total_goal)*100,2000);
        }
    }

    public void readData(DatabaseReference ref, final fragment_record_achievements.OnGetDataListener listener) {
        listener.onStart();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onSuccess(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onFailure();
            }
        });
    }
}