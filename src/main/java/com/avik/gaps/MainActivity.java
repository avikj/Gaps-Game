package com.avik.gaps;
import android.app.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.graphics.*;
import android.content.*;
import android.widget.*;
import android.content.res.*;
import com.jirbo.adcolony.*;

import java.util.ArrayList;

public class MainActivity extends Activity
{
    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        gameView = new GameView(getApplicationContext(), getSharedPreferences("com.avik.gaps", Context.MODE_PRIVATE));
        super.onCreate(savedInstanceState);
        AdColony.configure(this, "version:1.1,store:google", "app2602fe20fce44544b8", "vz16db1d7eeb1746f886");
        AdColony.addV4VCListener(new AdColonyV4VCListener()
        {
            public void onAdColonyV4VCReward(AdColonyV4VCReward reward)
            {
                if(reward.success())
                {
                    gameView.coins+=5;
                    getSharedPreferences("com.avik.gaps", Context.MODE_PRIVATE).edit().putInt(gameView.coinsKey, gameView.coins).apply();
                    Log.d("AdColony", "reward success");
                }
            }
        });
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(gameView);

    }

    @Override
    protected void onPause()
    {
// TODO: Implement this method
        super.onPause();
        AdColony.pause();
        getSharedPreferences("com.avik.gaps", Context.MODE_PRIVATE).edit().putInt(gameView.coinsKey, gameView.coins).apply();
        //gameView.paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AdColony.resume(this);
    }

class GameView extends View{
    int coins;
    Handler handler=new Handler();
    Runnable runnable;
    Rect[][] rects;
    int screenWidth = getResources().getDisplayMetrics().widthPixels;
    int screenHeight = getResources().getDisplayMetrics().heightPixels;
    float yv;
    float y;
    SharedPreferences prefs;
    float anchory;
    int oldScore =-1;
    float x;
    int hiscore = -1;
    float anchorx;
    int xv;
    boolean dead;
    int rad;
    int framesPassed;
    boolean started = false;
    float translated;
    int wait;
    String hiScoreKey = "com.avik.gaps.hiscore";
    String coinsKey = "com.avik.gaps.coins";
    int score;
    ArrayList<Point> prevPoints = new ArrayList<Point>(0);
    int spacing =900*screenWidth/1080;
    boolean scoreLogged = false;
    GameButton startButton = new GameButton(screenWidth/5, 3*screenHeight/4, 4*screenWidth/5, 5*screenHeight/6, "START", Color.WHITE);
    GameButton adButton = new GameButton(screenWidth/5, 3*screenHeight/4-screenHeight/9, 4*screenWidth/5, 5*screenHeight/6-screenHeight/9, "watch video", Color.WHITE);
    int framespassed = 0;

    public GameView(Context c, SharedPreferences sp){
        super(c);
        prefs = sp;
        reset(-1);
        Log.d("Log", "logger works");
        runnable = new Runnable(){
            public void run(){
                if(started){
                    if(!dead){
                        checkCollisions();
                        y+=yv;
                        yv-=0.05*screenWidth/1080;
                        score=-(int)((y+500*screenWidth/1080-spacing)/spacing);
                        if(score<0)score=0;
                        translated+=yv;
                    }else{
                        if(!scoreLogged){
                            Log.d("Log", "current hiscore "+prefs.getInt(hiScoreKey, -1));
                            Log.d("Log", "attempting to save data.");
                            prefs.edit().putInt(hiScoreKey, Math.max(score, prefs.getInt(hiScoreKey, -1))).apply();
                            prefs.edit().putInt(coinsKey, coins+score/10).apply();
                            scoreLogged = true;
                        }
                        for(int i=0; i<rects.length; i++){
                            rects[i][0].right-=18;
                            rects[i][1].left+=18;
                        }
                        wait++;
                        //  if(wait>50)
                        reset(score);
                        if(x>screenWidth/2+10)
                            x-=14;
                        else if (x<screenWidth/2-10)
                            x+=14;
                    }
                    GameView.this.invalidate();
                }
                handler.postDelayed(this, 30);
            }
        };

        handler.postDelayed(runnable, 30);
    }
    public void reset(int prevScore){
//super(c);
        score=0;
        rects = new Rect[100][2];
        wait=0;
        yv = -20*screenWidth/1080;
        y = 1400*screenWidth/1080;
        anchory = y;
        x= screenWidth/2;
        hiscore =  prefs.getInt(hiScoreKey, -1);
        coins = prefs.getInt(coinsKey, 0);
        Log.d("coins", coins+"");
        anchorx = x;
        xv = 15;
        dead = false;
        rad =50*screenWidth/1080;
        framesPassed = 0;
        translated=0;
        scoreLogged=false;
        started=false;
        oldScore = prevScore;
        startButton.setColor(Color.WHITE);
        initializeRects();

    }
    public void checkCollisions(){
        for(int i =0; i<rects.length; i++){
            if(collision(rects[i][0])||collision(rects[i][1])){
                yv=0;
                dead=true;
            }
        }
    }
    public void initializeRects(){

        for(int i = 0; i<rects.length; i++){
            int shift =(int)( (Math.random()*1000-500)*screenWidth/1080);

            rects[i][0]=new Rect(-1000*screenWidth/1080, -i*spacing-500*screenWidth/1080,screenWidth/2-screenWidth*100/1080+shift, -i*spacing-400*screenWidth/1080);
            rects[i][1]=new Rect(screenWidth/2+100*screenWidth/1080+shift, -i*spacing-500*screenWidth/1080, screenWidth+1000*screenWidth/1080, -i*spacing-400*screenWidth/1080);

        }
    }
    Paint p = new Paint();
    boolean dragged=false;
    @Override
    protected void onDraw(Canvas canvas){
        int backgroundColor = Color.rgb(113,220,158);//Color.BLACK;//Color.HSVToColor(new float[]{(float)((x+700)/6%360), (float)0.4, (float)0.9});
        canvas.translate(0, -translated);
        setBackgroundColor(backgroundColor);
        super.onDraw(canvas);
        if(started){
            p.setColor(Color.WHITE);
            //p.setStrokeWidth(30);
            canvas.drawRect(x - rad, y - rad, x + rad, y + rad, p);
            for(int i =0; i<rects.length; i++){
                canvas.drawRect(rects[i][0], p);
                canvas.drawRect(rects[i][1], p);
            }
            p.setColor(backgroundColor);
            p.setTextSize((int) (rad * 1.4));
            canvas.drawText(score + "", x - 40*screenWidth/1080, y + 10*screenWidth/1080, p);
            p.setColor(Color.WHITE);
        }
        else{
            p.setColor(Color.WHITE);
            p.setTextSize(screenHeight / 4);
            if(oldScore>=0)
                drawCenteredText(oldScore+"", screenWidth/2, screenHeight/6, canvas, p);
            else{
                p.setTextSize(screenHeight/7);
                drawCenteredText("g a p s", screenWidth / 2, screenHeight / 6, canvas, p);
            }
            p.setTextSize(screenHeight / 20);
            drawCenteredText("best: " + hiscore, screenWidth / 2, screenHeight / 3, canvas, p);
            drawCenteredText("coins: " + coins, screenWidth / 2, screenHeight / 3+screenHeight/10, canvas,p);
            startButton.setTextColor(backgroundColor);
            startButton.draw(canvas);
            adButton.setTextColor(backgroundColor);
            adButton.draw(canvas);
        }//p.setColor(Color.BLACK);
//canvas.drawText(screenWidth+", "+screenHeight, 500, 500, p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(started){
            if(!dead){
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    anchorx =event.getX();
//anchory=event.getY();
                    invalidate();
                }
                if(event.getAction()==MotionEvent.ACTION_MOVE){
                    x =event.getX()-anchorx+x;
//y=event.getY()-anchory+y;
//anchory=event.getY();
                    anchorx=event.getX();
                    invalidate();
                }
            }
            if(x>screenWidth-rad)
                x=screenWidth-rad;
            if(x<rad)
                x=rad;
        }else{
            if(startButton.contains(new Point((int) event.getX(), (int) event.getY()))) {
                if(startButton.react(event)) {
                    started = true;
                    startButton.setColor(Color.LTGRAY);
                }
            }
            else
                startButton.setColor(Color.WHITE);
            if(adButton.contains(new Point((int) event.getX(), (int) event.getY()))) {
                if (adButton.react(event)) {
                    AdColonyV4VCAd ad = new AdColonyV4VCAd("vz16db1d7eeb1746f886");
                    Log.d("AdColony", "attempting to show ad.");
                    ad.show();
                }
            }
            else adButton.setColor(Color.WHITE);
            invalidate();
        }
        return true;
    }
    public boolean collision(Rect r){
        if(((x-rad>=r.left&&x-rad<=r.right)||(r.left>=x-rad&&r.left<=x+rad))&&((y-rad>=r.top&&y-rad<=r.bottom)||(r.top>=y-rad&&r.top<=y+rad))){
            return true;
        }
        return false;
    }
    public void drawCenteredText(String text, int x, int y, Canvas c, Paint p){
        Rect bounds = new Rect();
        p.getTextBounds(text, 0, text.length(), bounds);
        float base = y+bounds.height()/2;
        float leftBound = x - bounds.width()/2;
        c.drawText(text, leftBound, base, p);
    }
}
class GameButton{
    int left;
    int right;
    int top;
    int bottom;
    int color;
    String text;
    int backColor = Color.WHITE;
    public GameButton(int l, int t, int r, int b, String txt, int c){
        left = l; right = r;
        top = t;  bottom = b;
        color = c;
        text=txt;
    }
    public void setTextColor(int color){
        this.color = color;
    }
    public String getText(){
        return text;
    }
    public void draw(Canvas c){
        Paint p = new Paint();
        p.setColor(backColor);
        c.drawRect(left, top, right, bottom, p);//c.drawRoundRect(left, top, right, bottom, 20, 20, p);
        p.setColor(color);
        drawCenteredText(text, (left+right)/2, (top+bottom)/2, c, p);
    }
    public boolean contains(Point p){
        return p.x<right&&p.x>left&&
                p.y>top&&p.y<bottom;
    }
    public void drawCenteredText(String text, int x, int y, Canvas c, Paint p){
        p.setTextSize((bottom-top)*1/2);
        Rect bounds = new Rect();
        p.getTextBounds(text, 0, text.length(), bounds);

        float base = y+bounds.height()/2;
        float leftBound = x - bounds.width()/2;
        c.drawText(text, leftBound, base, p);
    }
    public void setColor(int c){
        backColor = c;
    }
    public boolean react(MotionEvent event){
        if(contains(new Point((int) event.getX(), (int) event.getY()))){
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    backColor = (Color.LTGRAY);
                    Log.d("button", "down");
                    break;
                case MotionEvent.ACTION_UP:
                    backColor = Color.WHITE;
                    Log.d("button","up");
                    return true;
                case MotionEvent.ACTION_MOVE:
                    backColor = (Color.LTGRAY);
                    break;
            }
        }
        return false;
    }
}
}