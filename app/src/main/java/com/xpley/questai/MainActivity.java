package com.xpley.questai;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.Button;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class MainActivity extends Activity {
 TextView log; long lastDown=0; long lastUp=0;
 void record(String s){ String stamp=new SimpleDateFormat("HH:mm:ss.SSS",Locale.US).format(new Date()); log.append("\n"+stamp+"  "+s); }
 @Override public void onCreate(Bundle b){super.onCreate(b); LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(24,24,24,24); TextView intro=new TextView(this);intro.setText("Quest 3S Button Tester\nPress the headset action button, then controller buttons. System-reserved buttons may not be delivered. No permissions, no background service.");intro.setTextSize(19);layout.addView(intro);Button clear=new Button(this);clear.setText("Clear log");layout.addView(clear);ScrollView scroll=new ScrollView(this);log=new TextView(this);log.setTextSize(15);log.setText("Ready. Waiting for key events...");scroll.addView(log);layout.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));clear.setOnClickListener(v->log.setText("Log cleared."));setContentView(layout);}
 @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){long now=android.os.SystemClock.uptimeMillis();record("DOWN key="+e.getKeyCode()+" name="+KeyEvent.keyCodeToString(e.getKeyCode())+" repeat="+e.getRepeatCount()+(now-lastUp<450?" DOUBLE?":""));lastDown=now;}else if(e.getAction()==KeyEvent.ACTION_UP){long now=android.os.SystemClock.uptimeMillis();record("UP key="+e.getKeyCode()+" held="+(now-lastDown)+"ms");lastUp=now;}return super.dispatchKeyEvent(e);}
 @Override public boolean dispatchGenericMotionEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS||e.getActionMasked()==MotionEvent.ACTION_BUTTON_RELEASE)record("MOTION action="+e.getActionMasked()+" button="+e.getActionButton()+" source="+e.getSource());return super.dispatchGenericMotionEvent(e);}
 @Override protected void onPause(){super.onPause();record("App paused (system may have taken focus)");}
 @Override protected void onResume(){super.onResume();if(log!=null)record("App resumed");}
}