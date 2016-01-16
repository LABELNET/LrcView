package cn.labelnet.weiget;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.ChoiceFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

/**
 * LrcView 基本思路 ： 1.加载txt 2.解析 ，给 和弦的最后加上 # 以标记 3.解析分别进行存储，当前行没有 # 号，存储 为 no ,为
 * 当前行歌词 4.解析有# 则进行 存储，切读取下一行进行存储 5.传入参数进行播放 6.切换 7.
 * 
 */
public class LrcView extends View {
	private static final String TAG = LrcView.class.getSimpleName();
	private static final int MSG_NEW_LINE = 0;
	private static final String MSG_KEY_NO = "no";
	// 数据
	private List<String> mLrcKeys;
	private List<String> mLrcTexts;
	private LrcHandler mHandler;
	// 绘制
	private Paint mNormalPaint;
	private Paint mCurrentPaint;
	// 初始化参数
	private float mTextSize;
	private float mDividerHeight;
	private long mAnimationDuration;

	private int mCurrentLine = 0;
	private float mAnimOffset;

	// 计数器
	private int mNext = -1;
	// 总行数
	private int rowNums = 0;
	// 和弦
	private String[] chords = null;
	//和弦集合,反向存储 和弦键及其位置
	private Map<String,Integer> chordsList=new HashMap<String, Integer>();

	// 和弦计数器
	private int chordsIndex = -1;
	// 当前的和弦键
	private String nowChord = MSG_KEY_NO;
	// 记录上一次执行的操作
	private int preX = 0;
	// 下一个和弦键的下标
	private int nextChordsIndex = chordsIndex;
	// 当前行
	private int nextCurrentRow = mCurrentLine;
	// 下一个和弦值
	private String nextChord = MSG_KEY_NO;

	// 回调接口
	private LrcViewInterface lrcViewInterface;
	
	
	//显示处理
	private Pattern p = Pattern.compile("\\s+");
	//绘制和弦的画笔
	private Paint chordPaint;
	private Paint nomalChordPaint;
	private Paint nowChordPaint;
	

	public void setLrcViewInterface(LrcViewInterface lrcViewInterface) {
		this.lrcViewInterface = lrcViewInterface;
	}

	public LrcView(Context context) {
		this(context, null);
	}

	public LrcView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	/**
	 * @param attrs
	 */
	private void init(AttributeSet attrs) {
		// 初始化
		mTextSize = 24.0f;
		mDividerHeight = 72.0f;
		mAnimationDuration = 1000;
		mAnimationDuration = mAnimationDuration < 0 ? 1000 : mAnimationDuration;

		mLrcKeys = new ArrayList<String>();
		mLrcTexts = new ArrayList<String>();
		WeakReference<LrcView> lrcViewRef = new WeakReference<LrcView>(this);
		mHandler = new LrcHandler(lrcViewRef);
		mNormalPaint = new Paint();
		mCurrentPaint = new Paint();
		mNormalPaint.setColor(Color.BLACK);
		mNormalPaint.setTextSize(mTextSize);
		mCurrentPaint.setColor(Color.RED);
		mCurrentPaint.setTextSize(mTextSize);		
		mCurrentPaint.setAntiAlias(true);
		mNormalPaint.setAntiAlias(true);
		
		
		chordPaint=new Paint();
		chordPaint.setTextSize(mTextSize);
		chordPaint.setColor(Color.RED);
		chordPaint.setAntiAlias(true);
		
		nomalChordPaint=new Paint();
		nomalChordPaint.setTextSize(mTextSize);
		nomalChordPaint.setColor(Color.BLACK);
		nomalChordPaint.setAntiAlias(true);
		
		nowChordPaint=new Paint();
		nowChordPaint.setTextSize(30.0f);
		nowChordPaint.setColor(Color.GREEN);
		nowChordPaint.setAntiAlias(true);
		
		//设置字体
		mCurrentPaint.setTypeface(Typeface.SANS_SERIF);
		mNormalPaint.setTypeface(Typeface.SANS_SERIF);
		chordPaint.setTypeface(Typeface.SANS_SERIF);
		nowChordPaint.setTypeface(Typeface.SANS_SERIF);
		nomalChordPaint.setTypeface(Typeface.SANS_SERIF);
		
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mLrcKeys.isEmpty() || mLrcTexts.isEmpty()) {
			return;
		}
		// 中心Y坐标
		float centerY = getHeight() / 2 + mTextSize / 2 + mAnimOffset;
		
		// 画当前行
		String currStr = mLrcTexts.get(mCurrentLine);
		String currKey = mLrcKeys.get(mCurrentLine);
		
		float currX = (getWidth() - mCurrentPaint.measureText(currStr)) / 2;
		if (!MSG_KEY_NO.equals(currKey)) {
			parseLrcKeyStr(canvas,currX,centerY-30,currKey,chordPaint,mCurrentLine);
		}
		canvas.drawText(currStr, currX, centerY, mCurrentPaint);

		// 画当前行上面的
		for (int i = mCurrentLine - 1; i >= 0; i--) {
			String upStr = mLrcTexts.get(i);
			String upKey = mLrcKeys.get(i);

			float upX = (getWidth() - mNormalPaint.measureText(upStr)) / 2;
			float upY = centerY - (mTextSize + mDividerHeight)
					* (mCurrentLine - i);
			if (!MSG_KEY_NO.equals(upKey)) {
				parseLrcKeyStr(canvas,upX,upY-30,upKey,nomalChordPaint,i);
			}
			canvas.drawText(upStr, upX, upY, mNormalPaint);
		}

		// 画当前行下面的
		for (int i = mCurrentLine + 1; i < mLrcKeys.size(); i++) {
			String downStr = mLrcTexts.get(i);
			String downKey = mLrcKeys.get(i);
			float downX = (getWidth() - mNormalPaint.measureText(downStr)) / 2;
			float downY = centerY + (mTextSize + mDividerHeight)
					* (i - mCurrentLine);
			if (!MSG_KEY_NO.equals(downKey)) {
				parseLrcKeyStr(canvas,downX,downY-30,downKey,nomalChordPaint,i);
			}
			canvas.drawText(downStr, downX, downY, mNormalPaint);
		}
	}


	
	
	/**
	 * 绘制
	 * @param canvas
	 * @param currX
	 * @param centerY
	 * @param lrcKey
	 * @param paint
	 * @param currentLine
	 */
	int nowLineChords=0;
	private String needChords=null;
	
	private synchronized void parseLrcKeyStr(Canvas canvas,float currX,float centerY,String lrcKey,Paint paint,int currentLine){
		
		
		//字符串方式分割
		String[] split = getSplitStringArray(lrcKey);
	    
	    float spaceLength=mCurrentPaint.measureText(" ");
	    if(currentLine==mCurrentLine){
	    	System.out.println("nowLineChords : "+nowLineChords+" chordsIndex:"+chordsIndex);
	    	Log.d("Lrc", "needChords: "+needChords + " ");
	    }
	    
		float length=0;
		float sl=0;
		for(int i=0;i<split.length;i++){
			
			//不为""的两个字符之间添加 长度/2个全角空格
			//1.计数空格
			String str = split[i];
			if(str.equals("")||str==""){
				//是空格
				canvas.drawText(" ",currX+length, centerY, paint);
				length+=spaceLength*2;
			}else{
				float f = paint.measureText(str);
				
				
				//半角的
				if(f>30.0f&&f<40.0f){
					sl=f/5*2;
				}else if(f>=40.0f){
					sl=f/4;
				}else{
					sl=f/2;
				}
				
				length+=sl;
				//确定当前的和弦，1,和弦值一样，2,当前值一样 ,3,
				if(needChords.equals(str) && mCurrentLine==currentLine && nowLineChords==chordsIndex){
					  str=str.substring(0,str.length()-1);
					  canvas.drawText(str,currX+length, centerY, nowChordPaint);
				}else{
					str=str.substring(0,str.length()-1);
				    canvas.drawText(str,currX+length, centerY, paint);
				}
				
				length=length-sl+f;
			}
			
		}
	}

	/**
	 * 得到字符串数组
	 * @param lrcKey
	 * @return
	 */
	private String[] getSplitStringArray(String lrcKey) {
		String[] split = lrcKey.split(" ");
	    int splitIndex=0;
	    for(int i=0;i<split.length;i++){
	    	if(!split[i].equals("")){
	    		split[i]=split[i]+splitIndex;
	    		splitIndex++;
	    	}
	    }
		return split;
	}

	// 功能：字符串全角转换为半角
	// 说明：全角空格为12288，半角空格为32
//	 		 其他字符全角(65281-65374)与半角(33-126)的对应关系是：均相差65248 
	// 输入参数：input -- 需要转换的字符串
	// 输出参数：无：
	// 返回值: 转换后的字符串
	public static String fullToHalf(String input) 
	{  
		char[] c = input.toCharArray();  
		for (int i = 0; i< c.length; i++) 
		{  
		       if (c[i] == 12288) //全角空格
		       {  
		       		c[i] = (char) 32;  
		         	continue;  
		       }
	 
		       if (c[i]> 65280&& c[i]< 65375)  
		          c[i] = (char) (c[i] - 65248);  
		}  
		return new String(c);  
	}
	
	// 功能：字符串半角转换为全角
	// 说明：半角空格为32,全角空格为12288.
	// 其他字符半角(33-126)与全角(65281-65374)的对应关系是：均相差65248
	// 输入参数：input -- 需要转换的字符串
	// 输出参数：无：
	// 返回值: 转换后的字符串
	public static String halfToFull(String input) {
		char[] c = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == 32) // 半角空格
			{
				c[i] = (char) 12288;
				continue;
			}

			// 根据实际情况，过滤不需要转换的符号
			// if (c[i] == 46) //半角点号，不转换
			// continue;

			if (c[i] > 32 && c[i] < 127) // 其他符号都转换为全角
				c[i] = (char) (c[i] + 65248);
		    }
		return new String(c);
	}

	public void loadSDLrc(String lrcName) throws Exception {
		mLrcTexts.clear();
		mLrcKeys.clear();

		File file = new File(Environment.getExternalStorageDirectory(), lrcName);
		BufferedReader br = new BufferedReader(new FileReader(file));

		String line;
		int index = 0;
		while ((line = br.readLine()) != null) {
			// 单行加载解析
			// line = halfToFull(line);
			// 1.判断最后是否有 $ 符号
			index = line.lastIndexOf("$");
			if (index > 0) {
				// 则此行为 和弦行，进行解析，存储下一行数据
				// 存储和弦值
				line = line.substring(0, index);
				mLrcKeys.add(line);
				// 存储对应的歌词
				line = br.readLine();
				if (line != null) {
					mLrcTexts.add(line);
				} else {
					break;
				}

			} else {
				// 没有$符号，存储歌词，存储和弦为no
				mLrcKeys.add(MSG_KEY_NO);

				mLrcTexts.add(line);
			}

		}
		br.close();
		// 记录总行数
		rowNums = mLrcTexts.size();

		for (int i = 0; i < mLrcKeys.size() - 1; i++) {
			Log.d("Lrc", mLrcKeys.get(i));
		}
		Log.d("Lrc", " mLrcKeys : " + mLrcKeys.size());
		Log.d("Lrc", " mLrcTexts : " + mLrcTexts.size());
	}

	/**
	 *
	 * @param lrcName
	 *            assets下的歌词文件名
	 * @throws Exception
	 */
	public void loadLrc(String lrcName) throws Exception {
		mLrcTexts.clear();
		mLrcKeys.clear();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				getResources().getAssets().open(lrcName)));
		String line;
		int index = 0;
		while ((line = br.readLine()) != null) {
			// 单行加载解析
			// 
			// 1.判断最后是否有 $ 符号
			index = line.lastIndexOf("$");
			if (index > 0) {
				// 则此行为 和弦行，进行解析，存储下一行数据

				// 存储和弦值
				line = line.substring(0, index);
				
				mLrcKeys.add(line);
				// 存储对应的歌词
				line = br.readLine();
				if (line != null) {
					mLrcTexts.add(line);
				} else {
					break;
				}

			} else {
				// 没有$符号，存储歌词，存储和弦为no
				mLrcKeys.add(MSG_KEY_NO);
				Matcher m = p.matcher(line);
				line= m.replaceAll("　");
				mLrcTexts.add(line);
			}

		}
		br.close();
		// 记录总行数
		rowNums = mLrcTexts.size();

		for (int i = 0; i < mLrcKeys.size() - 1; i++) {
			Log.d("Lrc", mLrcKeys.get(i));
		}
		Log.d("Lrc", " mLrcKeys : " + mLrcKeys.size());
		Log.d("Lrc", " mLrcTexts : " + mLrcTexts.size());
	}

	/**
	 * 当一行的和弦执行全部切换完毕的时候，进行下一行切换 基本思路： 内部计数器，总行数; 1. x=1 ，进行下一个和弦的更新
	 * 
	 * 2.x=-1，进行上一个和旋的更新 3. 先得到 当前行的所有和弦， 如果有和弦
	 * ：将和弦进行解析，后有一个计数器，进行计数；当计数器没有等于和弦总个数的时候，进行下一行显示 如果没有和弦，直接进行下一行显示
	 * 下一行显示之前，先进行和弦判断
	 * 
	 * @param x
	 *            , -1 , 1
	 */
	public synchronized void updateLrc(int x) {
		
		
		Log.d("Lrc","-----------------------2---------------------------");
		Log.d("Lrc", "chordsIndex 1: " + chordsIndex);
		
		//临界值1，第一行
		if(x<0){
			if(mNext==0){
				//判定有和弦没有
				if(chordsIndex==-1){
					//没有和弦
					lrcViewInterface.isPlayToTop(true);
					return;
				}else{
					//有和弦
					if(preX<0){
						if(chordsIndex==0){
							//第一个
							Log.d("Lrc", "临界值1 : ");
							chordsIndex=1;
							lrcViewInterface.isPlayToTop(true);
							return;
						}
					}
					if(preX>0){
						if((chordsIndex-1)==0){
							//判定是不是第一个元素
							Log.d("Lrc", "临界值2: ");
							lrcViewInterface.isPlayToTop(true);
							return;
						}
					}
				}
			}
		}
		
		//临界值2： 最后一行了，不在进行更新操作
		if (x > 0) {
			if (mNext >= rowNums - 1) {
				//判定是否有和弦
				if(chordsIndex==-1){
					//没有和弦
					lrcViewInterface.isPlayToEnd(true);
					return;
				}else{
					//有和弦,判定是否等于数组长度
					if(chordsIndex==chords.length){
						//最后一个和弦了
						lrcViewInterface.isPlayToEnd(true);
						return;
					}
				}
				
			}
		}
	
		

		
		if (chordsIndex == -1) {
			// 如果chordsIndex等于-1 ，代表着 这一行没有 和弦，则进行滚动到下一行
			// x是1还是-1，如果是1，则代表向下一行，是-1则代表向上一行；
			parseLine(x);
		}

		Log.d("Lrc", "chordsIndex 2: " + chordsIndex);

		if (chords != null) {

			// 判断当前执行的动作和上一次执行的动作是否一样，
			// 如果一样，不进行chordsIndex操作，如果不一样，分别对其进行不同的操作
			if (preX == -1 && x == 1)  {
				if(chordsIndex != 0){
				    chordsIndex += 1;
				}
			} else if (preX == 1 && x == -1) {
				chordsIndex -= 1;
			}

			// 不等于空，开始控制
			switch (x) {
			case 1:
				// 下一个和弦的切换，更新界面，不更新行
				// 最后一个和弦
				if (chords.length == chordsIndex) {
					// 临界值为最大的时候，切换下一行
					parseLine(1);
					if (chordsIndex > -1) {
						nowChord = chords[chordsIndex];
						Log.d("Lrc", "和弦 +1 if: " + nowChord);
						chordsIndex++;
					}
				} else {
					nowChord = chords[chordsIndex];
					Log.d("Lrc", "和弦 +1 else : " + nowChord);
					chordsIndex++;
				}

				 Log.d("Lrc", "chordsIndex 3 : " + chordsIndex);
				 needChords=nowChord+(chordsIndex-1);

				break;
			case -1:

				chordsIndex--;
				chordsIndex = chordsIndex < -1 ? -1 : chordsIndex;
				// 上一个和弦的切换，更新界面，不更新行
				// Log.d("Lrc", "和弦 -1 执行了");

				Log.d("Lrc", "chordsIndex -3: " + chordsIndex);

				if (chordsIndex == -1) {
					// 临界值为最小的时候，切换上一行
					parseLine(-1);
					if (chordsIndex > -1) {
						chordsIndex--;
						nowChord = chords[chordsIndex];
						Log.d("Lrc", "和弦 if -1 : " + nowChord);
					}
				} else {
					nowChord = chords[chordsIndex];
					Log.d("Lrc", "和弦 else -1 : " + nowChord);
				}

				Log.d("Lrc", "chordsIndex -4: " + chordsIndex);
				 needChords=nowChord+chordsIndex;

				break;
			}
			
			// 这里是上面绘制，将单个和弦绘制的控制条件
			nowLineChords=chordsIndex;
//			if(x>0){
//			  needChords=nowChord+(chordsIndex-1);
//			}
//			
//			if(x<0){
//			
//			}
			
		} else {
			Log.d("Lrc", "和弦数组chords 为空了: ");	
		}
		preX = x;
		// 获得下一个和弦键
		nextChordsIndex = x > 0 ? chordsIndex : chordsIndex + 1;
		getNextChord();
		// 和弦回调
		lrcViewInterface.getNowChordAndNextChord(nowChord, nextChord);
		
		
		Log.d("Lrc", "needChords: "+needChords);
		
		
		invalidate();
		
		
	}

	/**
	 * 1. 解析当前行的和弦
	 */
	private void parseLine(int isNext) {
		if (isNext > 0) {
			mNext++;
		} else {
			mNext--;
		}
		mCurrentLine = mNext == rowNums ? 0 : mNext;
		mCurrentLine = mNext < 1 ? 0 : mNext;
		mNext = mCurrentLine;
		chords = null;
		chordsList.clear();
		// 获得当前的和弦
		String key = mLrcKeys.get(mCurrentLine);
		// 解析
		if (MSG_KEY_NO.equals(key)) {
			chordsIndex = -1;
			// 没有和弦，更新下一行
			mHandler.sendEmptyMessage(MSG_NEW_LINE);
		} else {
			// 有和弦
			chords = key.trim().split("\\s+");
			// 测试
			String t = " ";
			for (int i = 0; i < chords.length; i++) {
				t += chords[i] + " ";
				
				chordsList.put(chords[i]+i,i);
			}

			Log.d("Lrc", "和弦 :" + t);
			Log.d("Lrc", "歌词 :" + mLrcTexts.get(mCurrentLine));
			// 如果进行下一行，则
			if (isNext < 0) {
				chordsIndex = chords.length;
			} else {
				chordsIndex = 0;
			}
			mHandler.sendEmptyMessage(MSG_NEW_LINE);
		}
	}

	// 3. 获得下一个和弦值
	/*
	 * 基本思路：
	 * 
	 * 根据当前和弦键的下标+1=下一个和弦下标， 如果下标==数组长度， 则向下循环遍历 下面的和弦，取得最近的和弦返回即可
	 * 
	 * ！=数组长度，返回即可
	 */
	public void getNextChord() {
		if (nextChordsIndex == -1) {
			return;
		}
		if (chords != null) {
			if (nextChordsIndex >= chords.length) {
				// 当前行+1
				nextCurrentRow = mCurrentLine + 1;

				for (int i = nextCurrentRow; i < rowNums; i++) {
					String lrcKey = mLrcKeys.get(i);
					if (lrcKey.equals(MSG_KEY_NO)) {
						continue;
					} else {
						// 有和弦的行，取得第一个返回
						nextChord = lrcKey.trim().split("\\s+")[0];
						break;
					}
				}

			} else {
				if (nextChordsIndex > 0) {
					nextChord = chords[nextChordsIndex];
				}
			}
		}
	}

	/**
	 * 换行动画 Note:属性动画只能在主线程使用
	 */
	private void newLineAnim() {
		ValueAnimator animator = ValueAnimator.ofFloat(mTextSize
				+ mDividerHeight, 0.0f);
		animator.setDuration(mAnimationDuration);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				mAnimOffset = (Float) animation.getAnimatedValue();
				invalidate();
			}
		});
		animator.start();
	}

	private static class LrcHandler extends Handler {
		private WeakReference<LrcView> mLrcViewRef;

		public LrcHandler(WeakReference<LrcView> lrcViewRef) {
			mLrcViewRef = lrcViewRef;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_NEW_LINE:
				LrcView lrcView = mLrcViewRef.get();
				if (lrcView != null) {
					lrcView.newLineAnim();
				}
				break;
			}
			super.handleMessage(msg);
		}
	}

	// 回调接口
	public interface LrcViewInterface {
		// 回调出来 当前的和弦，和下一个和弦
		void getNowChordAndNextChord(String nowChord, String nextChord);

		void isPlayToEnd(boolean isToEnd);
		
		void isPlayToTop(boolean isToTop);
	}

}