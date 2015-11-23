package org.zywx.wbpalmstar.plugin.uextent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.tencent.weibo.sdk.android.api.WeiboAPI;
import com.tencent.weibo.sdk.android.api.util.Util;
import com.tencent.weibo.sdk.android.component.Authorize;
import com.tencent.weibo.sdk.android.component.sso.AuthHelper;
import com.tencent.weibo.sdk.android.component.sso.OnAuthListener;
import com.tencent.weibo.sdk.android.component.sso.WeiboToken;
import com.tencent.weibo.sdk.android.model.AccountModel;
import com.tencent.weibo.sdk.android.model.BaseVO;
import com.tencent.weibo.sdk.android.model.ModelResult;
import com.tencent.weibo.sdk.android.network.HttpCallback;

@SuppressWarnings("deprecation")
public class EUExTent extends EUExBase implements HttpCallback, Serializable {

	private static final long serialVersionUID = 5655082989178345497L;

	private static final String TAG = "EUExTent";

	private static final int MAX_SIZE = 1024 * 1024 * 5;

	private static final String CALLBACK = "uexTent.cbShare";

	private static final String function_cbRegisterApp = "uexTent.cbRegisterApp";
    private static final int INTENT_REGISTER_CALLBACK = 1;

    private String accessToken;
	private WeiboAPI weiboAPI;
	private AccountModel account;
	private ProgressDialog dialog;
	private Location mLocation;
	private Bitmap mBitmap;
	private static final int REGISTER_ERROR=1;//注册错误
	private static final int AUTHORIZATION_ERROR=2;//授权错误


    private static final String BUNDLE_DATA = "data";
    private static final int MSG_REGISTER_APP = 1;
    private static final int MSG_SEND_TEXT_CONTENT = 2;
    private static final int MSG_SEND_IMAGE_CONTENT = 3;

	public EUExTent(Context arg0, EBrowserView arg1) {
		super(arg0, arg1);
		EUExUtil.init(arg0);
	}

    public void registerApp(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_REGISTER_APP;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void registerAppMsg(String[] args) {
        final String appKey = args[0];
        final String appSecret = args[1];
        final String registerUrl = args[2];
        switch (checkAccount()) {
            case REGISTER_ERROR:// 没有注册
                Util.saveSharePersistent(mContext, "appKey", appKey);// 保存key
                Util.saveSharePersistent(mContext, "appSecret", appSecret);// 保存secret
                Util.saveSharePersistent(mContext, "registerUrl", registerUrl);// 保存url
                auth(Long.valueOf(appKey), appSecret, registerUrl);
                break;
            case AUTHORIZATION_ERROR:// 授权过期
                AuthHelper.unregister(mContext);
                auth(Long.valueOf(appKey), appSecret, registerUrl);
                break;
            default:
                jsCallback(function_cbRegisterApp, 0, EUExCallback.F_C_INT, EUExCallback.F_C_SUCCESS);// data=1,表示失败，data=0，表示成功
                break;
        }
        Log.i(TAG, "register");
    }

    public void sendTextContent(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_SEND_TEXT_CONTENT;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void sendTextContentMsg(String[] args) {
        Log.i(TAG, "sendTextContent");
        //如果授权过期,就反注册并提示
        switch (checkAccount()) {
            case REGISTER_ERROR:// 未注册
                jsCallback(CALLBACK, 0, 0, EUExCallback.F_C_FAILED);//回调分享失败
                return;
            case AUTHORIZATION_ERROR:// 授权过期
                AuthHelper.unregister(mContext);
                auth(Long.valueOf(getSharePersistent("appKey")), getSharePersistent("appSecret"), getSharePersistent("registerUrl"));
                break;
        }
        if (args == null || args.length < 1) {
            toast("plugin_tencent_parmeters_error");
            return;
        }

        if (args[0] == null || args[0].equals("")) {
            toast("plugin_tencent_content_cannot_be_null");
            return;
        }

        boolean isSendImage = false;
        String text = args[0];
        sendData(isSendImage, text);
    }

    public void sendImageContent(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_SEND_IMAGE_CONTENT;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void sendImageContentMsg(String[] args) {
        Log.i(TAG, "sendImageContent");
        //如果授权过期,就反注册并提示
        switch (checkAccount()) {
            case REGISTER_ERROR:// 未注册
                jsCallback(CALLBACK, 0, 0, EUExCallback.F_C_FAILED);//回调分享失败
                return;
            case AUTHORIZATION_ERROR:// 授权过期
                AuthHelper.unregister(mContext);
                auth(Long.valueOf(getSharePersistent("appKey")), getSharePersistent("appSecret"), getSharePersistent("registerUrl"));
                break;
        }
        if (args == null || args.length < 2) {
            toast("plugin_tencent_parmeters_error");
            return;
        }

        if (TextUtils.isEmpty(args[0])) {
            toast("plugin_tencent_image_path_cannot_be_null");
            return;
        }
        String path = args[0];
        String content = args[1] == null ? "" : args[1];
        path = TentUtils.getAbsPath(path, mBrwView);
        boolean isSendImage = true;

        if (path != null) {
            if (path.startsWith("/")) {
                File file;
                if (path == null || !(file = new File(path)).exists()) {
                    toast("plugin_tencent_file_not_exists");
                    return;
                }

                if (file.length() > MAX_SIZE) {
                    toast("plugin_tencent_file_too_large");
                    return;
                }

                mBitmap = BitmapFactory.decodeFile(path);
            } else {
                InputStream is;
                try {
                    is = mContext.getResources().getAssets().open(path);
                    if (is == null) {
                        toast("plugin_tencent_file_not_exists");
                        return;
                    }

                    if (is.available() >= MAX_SIZE) {
                        toast("plugin_tencent_file_too_large");
                        return;
                    }
                    mBitmap = BitmapFactory.decodeStream(is);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mBitmap == null) {
            toast("plugin_tencent_file_not_exists");
            return;
        }
        sendData(isSendImage, content);
    }

	/**
	 * 检测授权，授权过期就重新提交信息授权
	 * 
	 * @return true-注册或授权过期 false-注册或授权没有过期
	 */
	private int checkAccount() {
		accessToken = getSharePersistent("ACCESS_TOKEN");
		if (accessToken == null || "".equals(accessToken)) {
			return REGISTER_ERROR;// 没有注册
		}
		if (account == null)
			account = new AccountModel(accessToken);
		if (weiboAPI == null)
			weiboAPI = new WeiboAPI(account);
		if (weiboAPI != null)
			if (weiboAPI.isAuthorizeExpired(mContext)) {// 判断授权是否过期，过期了就返回true
				return AUTHORIZATION_ERROR;// 授权过期
			}
		return 0;
	}
	/**
	 * 通过key键得到保存的属性值
	 * 
	 * @param key 保存key值
	 *            
	 * @return 返回保存的属性
	 */
	private String getSharePersistent(String key) {
		return Util.getSharePersistent(mContext, key);
	}
	private void toast(String string) {
		if (string == null || string.equals("")) {
			return;
		}
		Toast.makeText(mContext, EUExUtil.getResStringID(string),
				Toast.LENGTH_SHORT).show();

	}

	private void auth(final long appid, String app_secket,
			final String redirectUri) {
		final Context context = mContext;
		// 注册当前应用的appid和appkeysec，并指定一个OnAuthListener
		// OnAuthListener在授权过程中实施监听
		AuthHelper.register(context, appid, app_secket, new OnAuthListener() {

			// 如果当前设备没有安装腾讯微博客户端，走这里
			@Override
			public void onWeiBoNotInstalled() {
                AuthHelper.unregister(mContext);
                Intent i = new Intent(mContext, Authorize.class);
                i.putExtra(Authorize.APP_KEY, String.valueOf(appid));
                i.putExtra(Authorize.REDIRECT_URI, redirectUri);

                try {
                    startActivityForResult(i, INTENT_REGISTER_CALLBACK);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }

			// 如果当前设备没安装指定版本的微博客户端，走这里
			@Override
			public void onWeiboVersionMisMatch() {
				AuthHelper.unregister(mContext);
				Intent i = new Intent(mContext, Authorize.class);
				i.putExtra(Authorize.APP_KEY, String.valueOf(appid));
				i.putExtra(Authorize.REDIRECT_URI, redirectUri);
                try {
                    startActivityForResult(i, INTENT_REGISTER_CALLBACK);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
			}

			// 如果授权失败，走这里
			@Override
			public void onAuthFail(int result, String err) {
				Toast.makeText(mContext, "result : " + result,
						Toast.LENGTH_LONG).show();
				AuthHelper.unregister(mContext);
				cbReister(false);
			}

			// 授权成功，走这里
			// 授权成功后，所有的授权信息是存放在WeiboToken对象里面的，可以根据具体的使用场景，将授权信息存放到自己期望的位置，
			// 在这里，存放到了applicationcontext中
			@Override
			public void onAuthPassed(String name, WeiboToken token) {
				Toast.makeText(mContext, "passed", Toast.LENGTH_LONG).show();
				//
				Util.saveSharePersistent(context, "ACCESS_TOKEN",
						token.accessToken);
				Util.saveSharePersistent(context, "EXPIRES_IN",
						String.valueOf(token.expiresIn));
				Util.saveSharePersistent(context, "OPEN_ID", token.openID);
				Util.saveSharePersistent(context, "REFRESH_TOKEN", "");
				Util.saveSharePersistent(context, "CLIENT_ID",
						String.valueOf(appid));
				Util.saveSharePersistent(context, "AUTHORIZETIME",
						String.valueOf(System.currentTimeMillis() / 1000l));
				AuthHelper.unregister(mContext);
				cbReister(true);
			}
		});

		AuthHelper.auth(mContext, "");
	}

	private void sendData(boolean isSendImage, String text) {
		String content = text;
		if ("".equals(content) && null == content) {
			toast("plugin_tencent_content_cannot_be_null");
			return;
		}
		if (dialog == null) {
			dialog = new ProgressDialog(mContext);
			dialog.setMessage(mContext.getString(EUExUtil
					.getResStringID("plugin_tencent_sending")));
		}

		if (mLocation == null) {
			loction();
		}

		if (content.length() >= 140) {
			toast("plugin_tencent_input_char_number_less_140");
		} else {
            try {
                content = URLEncoder.encode(text, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
			accessToken = Util.getSharePersistent(mContext, "ACCESS_TOKEN");
			if (accessToken == null || "".equals(accessToken)) {
				toast("plugin_tencent_auth");
				return;
			}

			if (account == null) {
				account = new AccountModel(accessToken);
			}
			if (weiboAPI == null) {
				weiboAPI = new WeiboAPI(account);
			}

			if (dialog != null && !dialog.isShowing()) {
				dialog.show();
			}

			double longitude = 0d;
			double latitude = 0d;
			if (mLocation != null) {
				longitude = mLocation.getLongitude();
				latitude = mLocation.getLatitude();
			}

			if (!isSendImage) {
				weiboAPI.addWeibo(mContext, content, "json", longitude,
						latitude, 0, 0, this, null, BaseVO.TYPE_JSON);
			} else if (mBitmap != null) {
				weiboAPI.addPic(mContext, content, "json", longitude, latitude,
						mBitmap, 0, 0, this, null, BaseVO.TYPE_JSON);
			}
		}

	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int flag = msg.what;

			if (flag == 15) {
				System.out.println("定位失败");
			}
		}

	};

	private View mView;

	@Override
	protected boolean clean() {
		return true;
	}

	@Override
	public void onResult(Object object) {
		{
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
			}

			if (object != null) {
				ModelResult result = (ModelResult) object;
				if (result.isExpires()) {
					jsCallback(CALLBACK, 0, 0, EUExCallback.F_C_FAILED);
				} else {
					if (result.isSuccess()) {
						Log.d("发送成功", object.toString());
						jsCallback(CALLBACK, 0, 0, EUExCallback.F_C_SUCCESS);
					} else {
						jsCallback(CALLBACK, 0, 0, EUExCallback.F_C_FAILED);
					}
				}
			}
		}
	}

	private void loction() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				Message msg = handler.obtainMessage();
				msg.what = 15;
				if (mLocation == null) {
					mLocation = Util.getLocation(mContext);
					if (mLocation != null) {
						msg.what = 10;
					}
				}
				handler.sendMessage(msg);
				Looper.loop();
			}

		}).start();
	}


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_REGISTER_CALLBACK && data != null){
            cbReister(data.getBooleanExtra(TentUtils.RESULT_REGISTER, false));
        }
    }

	public void cbReister(boolean success) {
		if (success) {
			jsCallback(function_cbRegisterApp, 0, EUExCallback.F_C_INT,
					EUExCallback.F_C_SUCCESS);// data=1,表示失败，data=0，表示成功
		} else {
			jsCallback(function_cbRegisterApp, 0, EUExCallback.F_C_INT,
					EUExCallback.F_C_FAILED);// data=1,表示失败，data=0，表示成功
		}

	}
    @Override
    public void onHandleMessage(Message message) {
        if(message == null){
            return;
        }
        Bundle bundle=message.getData();
        switch (message.what) {

            case MSG_REGISTER_APP:
                registerAppMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_SEND_TEXT_CONTENT:
                sendTextContentMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_SEND_IMAGE_CONTENT:
                sendImageContentMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            default:
                super.onHandleMessage(message);
        }
    }
}
