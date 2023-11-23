package landroo.org.puzzle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;

public class MessageTool
{
	private ViewGroup view;
	private LayoutInflater layoutInflater;
	private Handler handler;
	private int displayWidth;
	private int displayHeight;
	private PopupWindow popupWindow;
	private Context context;
	private int callNo = 0;
	private int callYes = 0;
	private EditText editText;

	public int resMode = -1;
	public String resText = "";
	
	public MessageTool(Context c, ViewGroup v, LayoutInflater inflater, int w, int h, Handler handler)
	{
		this.context = c;
		this.view = v;
		this.layoutInflater = inflater;
		this.handler = handler;
		this.displayWidth = w;
		this.displayHeight = h;
	}
	
	// text popup window
	public void showMessagePoup(String sText, int no, int yes, int mode)
	{
		callNo = no;
		callYes = yes;

		View popupView = layoutInflater.inflate(R.layout.message_box, null);
		int w = displayWidth - displayWidth / 10;
		int h = displayHeight / 2;
		popupWindow = new PopupWindow(popupView, w, h);

		Bitmap bitmap = drawBack(w, h, 0, 0, false, false, context);
		BitmapDrawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, w, h);
		popupWindow.setBackgroundDrawable(drawable);
		popupWindow.setFocusable(false);
		popupWindow.setTouchable(true);
		popupWindow.setOutsideTouchable(false);

		editText = (EditText) popupView.findViewById(R.id.message_text);
		editText.setText(sText);
		
		resText = sText;
		ImageButton imgbtn1 = (ImageButton) popupView.findViewById(R.id.message_no);
		imgbtn1.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				resMode = 0;
				popupWindow.dismiss();
				if(callNo != 0)
					handler.sendEmptyMessage(callNo);
			}
		});
		
		ImageButton imgbtn2 = (ImageButton) popupView.findViewById(R.id.message_yes);
		imgbtn2.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				resMode = 1;
				popupWindow.dismiss();
				if(callYes != 0)
					handler.sendEmptyMessage(callYes);
				resText = editText.getText().toString();
			}
		});
		
		switch(mode)
		{
		case 1:
			imgbtn1.setVisibility(View.VISIBLE);
			imgbtn2.setVisibility(View.VISIBLE);
			break;
		case 2:
			imgbtn1.setVisibility(View.GONE);
			imgbtn2.setVisibility(View.VISIBLE);
			break;
		case 3:
			imgbtn1.setVisibility(View.GONE);
			imgbtn2.setVisibility(View.VISIBLE);

			popupWindow.setFocusable(true);
			popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
				@Override
				public void onDismiss() {
					if(resMode == -1)
						popupWindow.showAtLocation(view, Gravity.CENTER, 10, 10);
				}
			});

			editText.setBackgroundResource(R.drawable.border);
			editText.setEnabled(true);
			editText.setFocusable(true);
			editText.requestFocus();

			(new Handler()).postDelayed(new Runnable()
			{
				public void run()
				{
					editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
					editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
				}
			}, 200);

			break;
		}
		
		popupWindow.showAtLocation(view, Gravity.CENTER, 10, 10);
	}

	// draw drawer background
	public static Bitmap drawBack(int w, int h, int xOff, int yOff, boolean gr, boolean border, Context context)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL);

		int BLACK = 0xAA303437;
		int WHITE = 0xAAC5C6C7;

		int[] colors = new int[3];
		colors[0] = WHITE;
		colors[1] = BLACK;
		colors[2] = WHITE;

		LinearGradient gradient;
		if (gr) gradient = new LinearGradient(0, 0, w, 0, colors, null, android.graphics.Shader.TileMode.CLAMP);
		else gradient = new LinearGradient(0, 0, 0, h, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);

		RectF rect = new RectF();
		rect.left = -xOff;
		rect.top = -yOff;
		rect.right = w;
		rect.bottom = h;
		float rx = dipToPixels(context, 20);
		float ry = dipToPixels(context, 20);

		canvas.drawRoundRect(rect, rx, ry, paint);

		if (border)
		{
			paint.setStyle(Paint.Style.STROKE);
			paint.setShader(null);
			paint.setColor(0xff000000);
			paint.setStrokeWidth(dipToPixels(context, 3));
			canvas.drawRoundRect(rect, rx, rx, paint);
		}

		return bitmap;
	}

	// calculate pixel size
	public static float dipToPixels(Context context, float dipValue)
	{
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}

}
