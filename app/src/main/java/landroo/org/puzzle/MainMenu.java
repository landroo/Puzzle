package landroo.org.puzzle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.SeekBar;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MainMenu
{
	private static final String TAG = "MainMenu";
	private static final String NATURE = "accipiter_nisus;acrocephalus_arundinaceus;actitis_hypoleucos;aegosoma_scabricorne;alauda_arvensis;alburnoides_bipunctatus;alcedo_atthis;anas_platyrhynchos;anthus_campestris;ardea_cinerea;ardea_purpurea;asio_flammeus;astacus_astacus;barbus_peteny;bombina_bombina;botaurus_stellaris;buteo_buteo;certhia_brachydactyla;chlidonias_hybridus;ciconia_nigra;circus_aeruginosus;cobitis_elongatoides;cobitis_taenia;colchicum_autumnale;coturnix_coturnix;crex_crex;crocus_heuffelianus;dendrocopos_major;dryocopus_martius;egretta_alba;eleocharis_carniolica;emberiza_calandra;emberiza_citrinella;eriogaster_catax;erithacus_rubecula;falco_tinnunculus;fringilla_coelebs;fritillaria_meleagris;gagea_spathacea;galanthus_nivalis;gobio_albipinnatus;gobio_gobio;hydrocharis_morsus_ranae;lanius_collurio;lanius_excubitor;lathyrus_nissolia;lemna_minor;leucojum_vernum;lucanus_cervus;luscinia_megarhynchos;lutra_lutra;lycaena_dispar;maculinea_teleius;martes_martes;misgurnus_fosilis;motacilla_alba;motacilla_flava;myotis_daubentonii;myotis_emarginatus;myotis_myotis;numphar_lutea;pelobates_fuscus;pernis_apivorus;platanthera_bifolia;porzana_parva;rana_dalmatina;remiz_pendulinus;rhodeus_sericeus;riparia_riparia;sabanejewia_aurata;salix_alba;salix_fragilis;scilla_kladnii;sitta_europaea;sylvia_nisoria;triturus_dobrogicus;unio_crassus;utricularia_vulgaris;vanellus_vanellus;vipera_berus;vista1;vista2;vista3;vista4;vista5;vista6;vista7;vista8";

	private ViewGroup view;
	private LayoutInflater layoutInflater;
	private Handler handler;
	private int displayWidth;
	private int displayHeight;
	public PopupWindow popupWindow;
	private Context context;
	private GridView gridView;
	private SeekBar seekbar;

	private List<ImageView> imgList;

	public int resMode = -1;
	public int selNum = 0;

	public MainMenu(Context c, ViewGroup v, LayoutInflater inflater, int w, int h, Handler handler)
	{
		this.context = c;
		this.view = v;
		this.layoutInflater = inflater;
		this.handler = handler;
		this.displayWidth = w;
		this.displayHeight = h;
	}
	
	// text popup window
	public void showMessagePoup(String imgName, int pieceSize)
	{
		View popupView = layoutInflater.inflate(R.layout.main_menu, null);
		int w = displayWidth - displayWidth / 10;
		int h = displayHeight - displayHeight / 10;
		popupWindow = new PopupWindow(popupView, w, h);

		try {
			Bitmap bitmap = drawBack(w, h, 0, 0, false, false, context);
			BitmapDrawable drawable = new BitmapDrawable(bitmap);
			drawable.setBounds(0, 0, w, h);
			popupWindow.setBackgroundDrawable(drawable);
		}
		catch(OutOfMemoryError ex){
			Log.i(TAG, "Out of memory on create background");
		}
		popupWindow.setFocusable(false);
		popupWindow.setTouchable(true);
		popupWindow.setOutsideTouchable(false);
		popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
			}
		});

		Button btn1 = (Button) popupView.findViewById(R.id.menu_easy);
		btn1.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				resMode = 0;
				popupWindow.dismiss();
				handler.sendEmptyMessage(200);
			}
		});

		Button btn3 = (Button) popupView.findViewById(R.id.menu_hard);
		btn3.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				resMode = 2;
				popupWindow.dismiss();
				handler.sendEmptyMessage(201);
			}
		});

		RadioButton rb = (RadioButton)popupView.findViewById(R.id.radio_nature);
		rb.setOnClickListener(new View.OnClickListener() {
			  @Override
			  public void onClick(View v) {
				  String[] stringArray = NATURE.split(";");
				  gridView.setAdapter(new ImageListAdapter(context, stringArray));
			  }
		  }
		);

		rb = (RadioButton)popupView.findViewById(R.id.radio_photos);
		rb.setOnClickListener(new View.OnClickListener() {
			  @Override
			  public void onClick(View v) {
				  String lst = collectPhotos();
				  String[] stringArray = lst.split(";");
				  gridView.setAdapter(new ImageListAdapter(context, stringArray));
			  }
		  }
		);

		gridView = (GridView) popupView.findViewById(R.id.gridview);
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long iid) {
				selNum = position;
				String imgName = (String)gridView.getAdapter().getItem(position);

				Message msg = handler.obtainMessage();
				Bundle bundle = new Bundle();
				bundle.putString("image_name", imgName);
				bundle.putInt("size", seekbar.getProgress());
				bundle.putInt("what", 100);
				msg.setData(bundle);

				handler.sendMessage(msg);
			}
		});


		seekbar = (SeekBar) popupView.findViewById(R.id.menu_size);
		seekbar.setProgress(pieceSize);
		seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				String imgName = (String)gridView.getAdapter().getItem(selNum);
				Message msg = handler.obtainMessage();
				Bundle bundle = new Bundle();
				bundle.putString("image_name", imgName);
				bundle.putInt("size", seekbar.getProgress());
				bundle.putInt("what", 100);
				msg.setData(bundle);

				handler.sendMessage(msg);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		String[] stringArray = NATURE.split(";");
		gridView.setAdapter(new ImageListAdapter(context, stringArray));

		popupWindow.showAtLocation(view, Gravity.CENTER, 10, 10);
	}

	// draw drawer background
	public static Bitmap drawBack(int w, int h, int xOff, int yOff, boolean gr, boolean border, Context context)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
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

/*	private void showPictures(View popupView, String images, int imgRes, int height)
	{
		LinearLayout ll = (LinearLayout) popupView.findViewById(R.id.menu_pictures);
		ll.removeAllViews();

		if(!images.equals(""))
		{
			imgList = new ArrayList<>();
			String[] imgArr = images.split(";");

			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) ll.getLayoutParams();
			params.height = height;
			ll.setLayoutParams(params);

			try
			{
				for (String anImgArr : imgArr)
				{
					int resourceId = context.getResources().getIdentifier(anImgArr, "drawable", context.getPackageName());
					Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
					ImageView imageView = new ImageView(context);
					if (bitmap != null)
					{
						bitmap = resizeImage(bitmap, height);

						LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
						lp.setMargins(10, 10, 10, 10);

						imageView.setImageBitmap(bitmap);
						imageView.setLayoutParams(lp);
						imageView.setPadding(10, 10, 10, 10);
						imageView.setTag(anImgArr);
						if(imgRes == resourceId)
							imageView.setBackgroundResource(R.drawable.border_sel);
						else
							imageView.setBackgroundResource(R.drawable.border_dis);
						imageView.setId(resourceId);
						imageView.setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								for(ImageView ivl: imgList)
									ivl.setBackgroundResource(R.drawable.border_dis);

								ImageView iv = (ImageView)v;
								iv.setBackgroundResource(R.drawable.border_sel);

								Message msg = handler.obtainMessage();
								Bundle bundle = new Bundle();
								bundle.putInt("imageid", iv.getId());
								bundle.putInt("what", 100);
								msg.setData(bundle);

								handler.sendMessage(msg);
							}
						});
					}
					else
					{
						imageView.setImageResource(R.drawable.grid);
					}
					imgList.add(imageView);
					ll.addView(imageView);
				}
			}
			catch(OutOfMemoryError ex)
			{
				Log.i(TAG, "" + ex);
			}
		}

	}
*/
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


	// grid list
	public class ImageListAdapter extends BaseAdapter
	{
		private Context context;
		public final String[] imgNames;
		private Bitmap bitmap;
		private Bitmap[] mask = new Bitmap[4];
		private Bitmap[] track = new Bitmap[3];
		private Paint paint = new Paint();
		private File file;
		private BitmapFactory.Options bfo = new BitmapFactory.Options();

		public ImageListAdapter(Context context, String[] mobileValues)
		{
			this.context = context;
			this.imgNames = mobileValues;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			View gridView = convertView;
			try
			{
				if(gridView == null)
				{
					LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					gridView = inflater.inflate(R.layout.gridline, parent, false);
				}

				String imgName = imgNames[position];

				// set image based on selected text
				SquareImageView imageView = (SquareImageView) gridView.findViewById(R.id.grid_item_image);

				final int resourceId = context.getResources().getIdentifier(imgName, "drawable", context.getPackageName());
				if(resourceId != 0) {
					bfo.inSampleSize = 2;
					Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, bfo);
					imageView.setImageBitmap(bitmap);
				}
				else {
					bfo.inSampleSize = 10;
					file = new File(imgName);
					if (file.exists()) {
						bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bfo);
						imageView.setImageBitmap(bitmap);
					}
				}
			}
			catch(OutOfMemoryError ex)
			{
				System.gc();
				Log.i(TAG, "" + ex);
			}
			catch(Exception ex)
			{
				Log.i(TAG, "" + ex);
			}

			return gridView;
		}

		@Override
		public int getCount()
		{
			return imgNames.length;
		}

		@Override
		public Object getItem(int position)
		{
			return imgNames[position];
		}

		@Override
		public long getItemId(int position)
		{
			return 0;
		}
	}

	private File getPhotoDir()
	{
		File mediaStorageDir = null, dir;
		String[] dirList = getStorageDirectories();
		for(int i = dirList.length - 1; i >= 0; i--)
		{
			dir = new File(dirList[i] + "/DCIM");
			mediaStorageDir = new File(dir, "Camera");
			if(!mediaStorageDir.exists()) mediaStorageDir = new File(dir, "100MEDIA");
			if(!mediaStorageDir.exists()) mediaStorageDir = new File(dir, "100ANDRO");
		}

		return mediaStorageDir;
	}

	public static String[] getStorageDirectories()
	{
		// Final set of paths
		final Set<String> rv = new HashSet<String>();
		// Primary physical SD-CARD (not emulated)
		final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
		// All Secondary SD-CARDs (all exclude primary) separated by ":"
		final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
		// Primary emulated SD-CARD
		final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
		if(TextUtils.isEmpty(rawEmulatedStorageTarget))
		{
			// Device has physical external storage; use plain paths.
			if(TextUtils.isEmpty(rawExternalStorage))
			{
				// EXTERNAL_STORAGE undefined; falling back to default.
				rv.add("/storage/sdcard0");
			}
			else
			{
				rv.add(rawExternalStorage);
			}
		}
		else
		{
			// Device has emulated storage; external storage paths should have
			// userId burned into them.
			final String rawUserId;
			final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
			final String[] folders = Pattern.compile("/").split(path);
			final String lastFolder = folders[folders.length - 1];
			boolean isDigit = false;
			try
			{
				Integer.valueOf(lastFolder);
				isDigit = true;
			}
			catch(NumberFormatException ignored)
			{
			}
			rawUserId = isDigit ? lastFolder : "";

			// /storage/emulated/0[1,2,...]
			if(TextUtils.isEmpty(rawUserId))
			{
				rv.add(rawEmulatedStorageTarget);
			}
			else
			{
				rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
			}
		}
		// Add all secondary storages
		if(!TextUtils.isEmpty(rawSecondaryStoragesStr))
		{
			// All Secondary SD-CARDs splited into array
			final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
			Collections.addAll(rv, rawSecondaryStorages);
		}
		return rv.toArray(new String[rv.size()]);
	}

	private String collectPhotos(){
		StringBuilder sRet = new StringBuilder();
		try
		{
			// [/storage/emulated/0, /storage/extSdCard]
			String[] dirList = getStorageDirectories();
			for(int i = 0; i < dirList.length; i++)
			{
				// [/storage/emulated/0/DCIM/Camera, /storage/emulated/0/DCIM/.thumbnails]
				File[] imageFolders = new File(dirList[i] + "/DCIM").listFiles();
				for (int j = 0; j < imageFolders.length; j++)
				{
					if(imageFolders[j].isDirectory() && !imageFolders[j].getName().substring(0, 1).equals("."))
					{
						// [/storage/emulated/0/DCIM/Camera/IMG_ODBK.jpg, /storage/emulated/0/DCIM/Camera/20141219_191443.jpg, ...
						File[] imageFiles = imageFolders[j].listFiles();
						for(int k = 0; k < imageFiles.length; k++)
						{
							if(imageFiles[k].getAbsolutePath().toLowerCase().indexOf(".jpg") != -1)
							{
								sRet.append(imageFiles[k].getAbsolutePath());
								sRet.append(";");
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return sRet.toString();
	}
}
