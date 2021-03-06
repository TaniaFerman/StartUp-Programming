package com.danycabrera.signcoach;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ViewFlipper;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.view.SurfaceView;
import android.view.ViewGroup;

import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class LearnActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String question_string = "Show me a", lesson_string = "This is a";
    private static final String vowels = "AEFHILMNORSX";    //Letters which should be preceded by 'an'
    private TextView tv_question, tv_lesson;
    private ImageView iv_lesson;
    private ViewFlipper viewFlipper;
    private TestManager testManager;
    private Handler questionHandler;
    private LearnMessage current_message;
    boolean camera_setting, viewIsQuestion = false;
    int handed_setting;
	private static boolean camera_default, handed_default;
    private ProgressBar progressBar;
    private SharedPreferences prefs;
    private long startTime;
    private ProgressBarAnimation progressAnim;
	public Counter counter = new Counter();;
	public CameraWrapper cam = new CameraWrapper();


	static {
		System.loadLibrary("native-lib");

		if(!OpenCVLoader.initDebug()){
			Log.e("MainActivity", "OpenCV not loaded.");
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);
        questionHandler = new Handler();
        //---------Do drawer stuff-----------------------
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ViewStub stub = (ViewStub) findViewById(R.id.content_stub);
        stub.setLayoutResource(R.layout.content_learn);
        stub.inflate();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
       // drawer.
        //-----------------------------------------------

		// set up opencv camera wrapper
		JavaCameraView camView = (JavaCameraView) findViewById(R.id.camera_view);
		cam.init(camView, this, this.counter);

		tv_question = (TextView) findViewById(R.id.tv_question); //Handles for the two messages we'll be changing
        tv_lesson = (TextView) findViewById(R.id.tv_lesson);
        iv_lesson = (ImageView) findViewById(R.id.iv_lesson);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        viewFlipper = (ViewFlipper) findViewById(R.id.learn_flipper);
        viewFlipper.setInAnimation(this, R.anim.slide_in);
        viewFlipper.setOutAnimation(this, R.anim.slide_out);
        progressAnim = new ProgressBarAnimation(progressBar, 10000);
        testManager = new TestManager();
        testManager.setToolbar(toolbar);
    }


    private void setMessage() {

        if (current_message.isLesson()) {
            tv_lesson.setText(doGrammarCat(lesson_string, current_message.getString()));
            iv_lesson.setImageDrawable(findDrawable(current_message.getString()));
            moveToLessonView(null);
        } else {
            tv_question.setText(doGrammarCat(question_string, current_message.getString()));
            moveToQuestionView(null);
        }

		cam.currentCharacter = current_message.getChar();
    }

    private Drawable findDrawable(String sign) {
        switch (sign) {
            case "A":
                return getResources().getDrawable(R.drawable.a);
            case "B":
                return getResources().getDrawable(R.drawable.b);
            case "C":
                return getResources().getDrawable(R.drawable.c);
            case "D":
                return getResources().getDrawable(R.drawable.d);
            case "E":
                return getResources().getDrawable(R.drawable.e);
            case "F":
                return getResources().getDrawable(R.drawable.f);
            case "G":
                return getResources().getDrawable(R.drawable.g);
            case "H":
                return getResources().getDrawable(R.drawable.h);
            case "I":
                return getResources().getDrawable(R.drawable.i);
            case "J":
                return getResources().getDrawable(R.drawable.j);
            case "K":
                return getResources().getDrawable(R.drawable.k);
            case "L":
                return getResources().getDrawable(R.drawable.l);
            case "M":
                return getResources().getDrawable(R.drawable.m);
            case "N":
                return getResources().getDrawable(R.drawable.n);
            case "O":
                return getResources().getDrawable(R.drawable.o);
            case "P":
                return getResources().getDrawable(R.drawable.p);
            case "Q":
                return getResources().getDrawable(R.drawable.q);
            case "R":
                return getResources().getDrawable(R.drawable.r);
            case "S":
                return getResources().getDrawable(R.drawable.s);
            case "T":
                return getResources().getDrawable(R.drawable.t);
            case "U":
                return getResources().getDrawable(R.drawable.u);
            case "V":
                return getResources().getDrawable(R.drawable.v);
            case "W":
                return getResources().getDrawable(R.drawable.w);
            case "X":
                return getResources().getDrawable(R.drawable.x);
            case "Y":
                return getResources().getDrawable(R.drawable.y);
            case "Z":
                return getResources().getDrawable(R.drawable.z);
            default:
                return getResources().getDrawable(R.drawable.a);
        }
    }

    //Concatenates and does appropriate grammar for provided message and character
    private String doGrammarCat(String message, String c) {
        String vowel_grammar = " ";
        if (isVowel(c)) {
            vowel_grammar = "n ";
        }
        return message + vowel_grammar + c;
    }

    public void nextQuestion(View v) {
        current_message = testManager.getNextMessage();
        if (current_message == null) {
            getNextSet();
            current_message = testManager.getNextMessage();
        }
        setMessage();
        if (current_message.isLesson()) {
            moveToLessonView(null); //The sending view is not used to move views anyway, so null
        } else if(viewIsQuestion){
            moveToQuestionView(null);
        }
    }

    private void getNextSet() {
        //TODO: Show success screen
        Log.d("LearnActivity", "Current message is " + (testManager.getCurrentMessage().isLesson()? "lesson": "question") + " of " + testManager.getCurrentMessage().getChar());
        testManager.moveToNextSet();
    }

    private void returnResult(boolean result) {
        testManager.sendResult(result);
        if (testManager.currentSetCompleted())
            getNextSet();
    }

    private boolean isVowel(String c) {
        return vowels.contains(c);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.send_feedback){
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("plain/text");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "signcoachasl@gmail.com" });
            intent.putExtra(Intent.EXTRA_SUBJECT, "RE: SignCoach Feedback");
            startActivity(Intent.createChooser(intent, ""));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.stay_still, R.anim.blow_down);
        }
    }


	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			if (status == LoaderCallbackInterface.SUCCESS) {
				cam.enable();
			} else {
				super.onManagerConnected(status);
			}
		}
	};

    @Override
    public void onPause() {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(getString(R.string.current_set), testManager.getCurrentSetIndex());
        edit.commit();
        Log.d("LearnActivity", "onPause: current set is " + Integer.toString(testManager.getCurrentSetIndex()));
        super.onPause();
		cam.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getCurrentPreferences();
        camera_setting = getCurrentCameraSetting();
        int currentSet = prefs.getInt(getString(R.string.current_set), 0);
        Log.d("LearnActivity", "onResume current set is " + Integer.toString(currentSet));
        testManager.setSet(currentSet);
        Log.d("LearnActivity", "Attempting to show set " + Integer.toString(currentSet));
        nextQuestion(null);

		if (!OpenCVLoader.initDebug()) {
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
		} else {
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
    }

    public void getCurrentPreferences() {
        prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE);
        Log.d("LearnActivity", "STARTING ACTIVITY");
        camera_default = getResources().getBoolean(R.bool.camera_default);
        camera_setting = getCurrentCameraSetting();
        handed_setting = prefs.getInt(getString(R.string.handed_setting), 0);
    }

    private boolean getCurrentCameraSetting() {
        return prefs.getBoolean(getString(R.string.camera_setting), camera_default);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
		cam.disable();
    }


    private void moveToSuccessView(View v) {
        viewIsQuestion = false;
        //This is necessary because this function is not always caled
        //from the UI thread
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.success_screen)));
            }
        });
    }
    private void moveToFailureView(View v){
        viewIsQuestion = false;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.failure_screen)));
            }
        });
    }
    public void doSkip(View v){
        questionHandler.removeCallbacks(questionTimeOut);
        nextQuestion(v);
//        fakeSuccess(v);
    }
    public void fakeSuccess(View v) {
        returnResult(true);
        questionHandler.removeCallbacks(questionTimeOut);
        moveToSuccessView(v);
    }

    public void fakeFailure(View v) {
        questionHandler.removeCallbacks(questionTimeOut);
        returnResult(false);
        moveToFailureView(v);
    }

    /*
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        imgOriginal = inputFrame.rgba();
        // Process and return frame
        if(viewIsQuestion) {
            float r = processFrame(imgOriginal.getNativeObjAddr(), img.getNativeObjAddr(), (char) current_message.getString().getBytes()[0]);
            Log.i("onCameraFrame", "r = " + Float.toString(r));
            boolean yes = false;
            if (r > 0.9) {
                yes = true;
                trueCount++;
                Log.i("onCameraFrame", "This is an C!");
            } else {
                falseCount++;
                Log.i("onCameraFrame", "NOT C...");
            }
            updateCount(yes);
        }
        return imgOriginal;
    }*/

    private Runnable questionTimeOut = new Runnable() {
        @Override
        public void run() {
            Log.d("LearnActivity", counter.toString());
            if (counter.trueCount > 0) {
                fakeSuccess(null);
            }
            else {
                fakeFailure(null);
                Log.d("LearnActivity", "going to failure town");
            }
            counter.reset();
        }
    };
    private Runnable resultDelay = new Runnable() {
        @Override
        public void run() {
            counter.reset();
            questionHandler.postDelayed(questionTimeOut, 8000);
            questionHandler.removeCallbacks(resultDelay);
            Log.d("LearnActivity", "Finished delay period");
        }
    };

    public void moveToLessonView(View v) {
        viewIsQuestion = false;
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.lesson_view)));
    }

    public void moveToQuestionView(View v) {
        viewIsQuestion = true;
        counter.reset();
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.question_view)));
        questionHandler.postDelayed(resultDelay, 2000);
        progressBar.setProgress(0);
        progressAnim.setProgress(100);
        startTime = SystemClock.uptimeMillis();
        Log.d("LearnActivity", "Question started at" + Long.toString(startTime));
    }

    public static native float processFrame(long iAddr1, long iAddr2, char c);
}