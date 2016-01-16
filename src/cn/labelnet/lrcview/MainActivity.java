package cn.labelnet.lrcview;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cn.labelnet.weiget.LrcView;
import cn.labelnet.weiget.LrcView.LrcViewInterface;

public class MainActivity extends Activity implements OnClickListener,
		LrcViewInterface {

	private LrcView view_lrc;
	private Button btn_add, btn_remove;
	private TextView tv_nowChord, tv_nextChord;

	/**
	 * 控制已实现 还需要做的 ： 1. 对应关系及其显示 2. 歌词过长处理
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv_nowChord = (TextView) findViewById(R.id.tv_nowChord);
		tv_nextChord = (TextView) findViewById(R.id.tv_nextChord);

		btn_add = (Button) findViewById(R.id.btn_add);
		btn_add.setOnClickListener(this);
		btn_remove = (Button) findViewById(R.id.btn_remove);
		btn_remove.setOnClickListener(this);

		view_lrc = (LrcView) findViewById(R.id.view_lrc);
		view_lrc.setLrcViewInterface(this);
		try {
			// view_lrc.loadSDLrc("dang.txt");
			view_lrc.loadLrc("lrc.txt");
			view_lrc.updateLrc(1);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.btn_add:
			// 点击增加模拟
			view_lrc.updateLrc(1);
			break;
		case R.id.btn_remove:
			// 点击减少模拟
			view_lrc.updateLrc(-1);
			break;
		}

	}

	@Override
	public void getNowChordAndNextChord(String nowChord, String nextChord) {
		tv_nowChord.setText(nowChord);
		tv_nextChord.setText(nextChord);
	}

	@Override
	public void isPlayToEnd(boolean isToEnd) {
		if (isToEnd) {
			showToast("播放完毕了");
		}
	}

	public void showToast(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void isPlayToTop(boolean isToTop) {
		showToast("准备开始");
	}

	
	
	//音量键监听
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			// 音量键+
			view_lrc.updateLrc(1);
			
			break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			// 音量键-
			view_lrc.updateLrc(-1);
			break;
		}

		return super.onKeyDown(keyCode, event);
	}

}
