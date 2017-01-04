package looklook.lyb.com.myswipecaptcha;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import looklook.lyb.com.myswipecaptcha.View.SwipeCaptchaView;

public class MainActivity extends AppCompatActivity {

    private SeekBar mSeekBar;
    private SwipeCaptchaView mSwipeCaptchaView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSeekBar = (SeekBar) findViewById(R.id.dragBar);
        Button btnChange= (Button) findViewById(R.id.btnChange);
        mSwipeCaptchaView = (SwipeCaptchaView) findViewById(R.id.swipeCaptchanView);
        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeCaptchaView.refresh();
                mSeekBar.setProgress(0);
            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSwipeCaptchaView.setCurrentSwipteVaule(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //开始的时候设置最大值
                mSeekBar.setMax(mSwipeCaptchaView.getMaxSwipeValue());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSwipeCaptchaView.matchCaptcha();
            }
        });
        mSwipeCaptchaView.setOnCaptchaMatchCallback(new SwipeCaptchaView.OnCaptchaMatchCallback() {
            @Override
            public void matchSuccess() {
                Toast.makeText(MainActivity.this, "恭喜你啊 验证成功 可以搞事情了", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void matchFailed() {
                Toast.makeText(MainActivity.this, "你有80%的可能是机器人，现在走还来得及", Toast.LENGTH_SHORT).show();
                mSwipeCaptchaView.resetCaptcha();
                mSeekBar.setProgress(0);
            }
        });
    }
}
