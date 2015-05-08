package org.zywx.wbpalmstar.plugin.uextent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.widget.Toast;

public class TentUtils {
	
	public static final int FILE_MAX_SIZE = 1024*1024*5;
	public static int fileSize = 0;
	
	static{
		fileSize = 0;
	}
	
	public static void showToast(final Activity activity, final String msg){
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public static void showMsg(Context context, String resKey){
		String msg = context.getString(EUExUtil.getResStringID(resKey));
		showToast((Activity) context, msg);
	}
	
	public static String getStr(Context context, String resKey){
		return context.getString(EUExUtil.getResStringID(resKey));
	}
	
	public static void toast( Activity activity, final String msg){
		Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
	}
	
	

	public static String assetToSd(Context ctx, String inFileName,
			String outFileName) {
		
		try {
			InputStream is = ctx.getAssets().open(inFileName);
			if (is == null) {
				return null;
			}
			
			if (Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				String sd = Environment.getExternalStorageDirectory()
						.getAbsolutePath();
				String path = sd + "/uexTencent/";
				File filePath = new File(path);
				if (!filePath.exists()) {
					filePath.mkdirs();
				}
				String fileName = path + outFileName;
				FileOutputStream fos = new FileOutputStream(fileName);
				
				byte[] buf = new byte[1024];
				int len = 0;
				int total = 0;
				while ((len = is.read(buf)) != -1) {
					fos.write(buf, 0, len);
					total++;
				}
				is.close();
				fos.close();
				fileSize=total;
				return fileName;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getAbsPath(String path, EBrowserView mBrwView) {
		return BUtility.makeRealPath(path,
				mBrwView.getCurrentWidget().m_widgetPath,
				mBrwView.getCurrentWidget().m_wgtType);
	}
	
	public static String makeFile(Context context, String filePath)
	{
		String fileName;
		if (!filePath.startsWith("/")) {
			if (filePath.contains("/")) {
				fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
			} else {
				fileName = filePath;
			}
			return assetToSd(context, filePath, fileName);
		}
		return null;
	}
	 
	
	public static String getIp(Context ctx) {
		WifiManager wm = (WifiManager) ctx
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wi = wm.getConnectionInfo();
		int ip = wi.getIpAddress();
		String ipAddress = intToIp(ip);
		return ipAddress;
	}

	private static String intToIp(int i) {

		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}
}
