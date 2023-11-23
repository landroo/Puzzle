package landroo.org.puzzle;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import java.io.IOException;
import java.net.MalformedURLException;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class UrlImageView extends LinearLayout
{

	private Context mContext;
	private Drawable mDrawable;
	private ProgressBar mSpinner;
	private ImageView mImage;

	public UrlImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}

	public UrlImageView(Context context)
	{
		super(context);
		init(context);
	}

	/**
	 * First time loading of the LoaderImageView Sets up the LayoutParams of the
	 * view, you can change these to get the required effects you want
	 */
	private void init(final Context context)
	{
		mContext = context;

		mImage = new ImageView(mContext);
		mImage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mImage.setVisibility(View.GONE);

		mSpinner = new ProgressBar(mContext);
		mSpinner.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		mSpinner.setIndeterminate(true);

		addView(mSpinner);
		addView(mImage);
	}

	/**
	 * Set's the view's drawable, this uses the internet to retrieve the image
	 * don't forget to add the correct permissions to your manifest
	 * 
	 * @param imageUrl
	 *            the url of the image you wish to load
	 */
	public void setImageDrawable(final String imageUrl, final int width, final int height)
	{
		mDrawable = null;
		mSpinner.setVisibility(View.VISIBLE);
		mImage.setVisibility(View.GONE);
		new Thread()
		{
			public void run()
			{
				try
				{
					mDrawable = getDrawableFromUrl(imageUrl, width, height);
					imageLoadedHandler.sendEmptyMessage(RESULT_OK);
				}
				catch (MalformedURLException e)
				{
					imageLoadedHandler.sendEmptyMessage(RESULT_CANCELED);
				}
				catch (IOException e)
				{
					imageLoadedHandler.sendEmptyMessage(RESULT_CANCELED);
				}
			};
		}.start();
	}

	public void setImageDrawable(final int imageID, final int width, final int height)
	{
		mDrawable = null;
		mSpinner.setVisibility(View.VISIBLE);
		mImage.setVisibility(View.GONE);
		new Thread()
		{
			public void run()
			{
				try
				{
					mDrawable = getDrawableFromUrl(imageID, width, height);
					imageLoadedHandler.sendEmptyMessage(RESULT_OK);
				}
				catch (MalformedURLException e)
				{
					imageLoadedHandler.sendEmptyMessage(RESULT_CANCELED);
				}
				catch (IOException e)
				{
					imageLoadedHandler.sendEmptyMessage(RESULT_CANCELED);
				}
			};
		}.start();
	}


	/**
	 * Callback that is received once the image has been downloaded
	 */
	private final Handler imageLoadedHandler = new Handler(new Callback()
	{
		@Override
		public boolean handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case RESULT_OK:
				mImage.setImageDrawable(mDrawable);
				mImage.setVisibility(View.VISIBLE);
				mSpinner.setVisibility(View.GONE);
				break;
			case RESULT_CANCELED:
			default:
				// Could change image here to a 'failed' image
				// otherwise will just keep on spinning
				break;
			}
			return true;
		}
	});

	/**
	 * Pass in an image url to get a drawable object
	 * 
	 * @return a drawable object
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private Drawable getDrawableFromUrl(final String url, final int width, final int height) throws IOException, MalformedURLException
	{
		//return Drawable.createFromStream(((java.io.InputStream) new java.net.URL(url).getContent()), "name");
		Bitmap bitmap = decodeSampledBitmapFromUri(url, width, height);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, width, height);
		return drawable;
	}

	private Drawable getDrawableFromUrl(final int id, final int width, final int height) throws IOException, MalformedURLException
	{
		//return Drawable.createFromStream(((java.io.InputStream) new java.net.URL(url).getContent()), "name");
		Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), id);
		bitmap = resizeImage(bitmap, height);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, width, height);
		return drawable;
	}

	
	public static Bitmap decodeSampledBitmapFromUri(String path, int reqWidth, int reqHeight)
	{
		Bitmap bm = null;

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		bm = BitmapFactory.decodeFile(path, options);

		return bm;
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{
			if (width > height) inSampleSize = Math.round((float) height / (float) reqHeight);
			else inSampleSize = Math.round((float) width / (float) reqWidth);
		}

		return inSampleSize;
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); // Snap to width
	}

	private Bitmap resizeImage(Bitmap bitmap, int maxWidth)
	{
		int origWidth = bitmap.getWidth();
		int origHeight = bitmap.getHeight();
		float newheight = (float) maxWidth / (float) origWidth * (float) origHeight;
		float scaleWidth = ((float) maxWidth) / origWidth;
		float scaleHeight = newheight / origHeight;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, origWidth, origHeight, matrix, false);

		return bitmap;
	}
}
