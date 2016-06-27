package com.itheima.ipdail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;

public class MainActivity extends ListActivity {
	private TextView tv_info;
	private SMSContentObserver smsContentObserver;
	private CallLogObserver callLogObserver;
	private PhoneStateReceiver myReceiver;

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			String msgBody = (String) msg.obj;
			tv_info.setText(msg.obj + ":" + msgBody);
		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] array = { "注册短信数据库变化的观察者", "收件箱数据库……", "删除新来电的通话记录", "监听新来电通话记录的详细信息", "取消注册Observer",//
				"注册电话状态改变的广播，当有来电时立即挂断电话", "取消注册广播", };
		for (int i = 0; i < array.length; i++) {
			array[i] = i + "、" + array[i];
		}
		ListAdapter mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(Arrays.asList(array)));
		tv_info = new TextView(this);// 将内容显示在TextView中
		tv_info.setTextColor(Color.BLUE);
		tv_info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		tv_info.setPadding(20, 10, 20, 10);
		getListView().addFooterView(tv_info);
		setListAdapter(mAdapter);
		myReceiver = new PhoneStateReceiver();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		switch (position) {
		case 0:
			smsContentObserver = new SMSContentObserver(mHandler, this, SMSContentObserver.MSG_SMS_WHAT);
			getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, smsContentObserver);
			// boolean notifyForDescendents(后裔)：若为true，则监视所有以指定的Uri开头的Uri；若为false，则只精确的监视指定的URI
			break;
		case 1:
			smsContentObserver = new SMSContentObserver(mHandler, this, SMSContentObserver.MSG_SMS_INBOX_WHAT);
			getContentResolver().registerContentObserver(Uri.parse("content://sms/inbox"), true, smsContentObserver);
			break;
		case 2:
			callLogObserver = new CallLogObserver(mHandler, this, CallLogObserver.MSG_CALLLOG_DELETE_WHAT);
			getContentResolver().registerContentObserver(Uri.parse("content://call_log/calls"), true, callLogObserver);
			break;
		case 3:
			callLogObserver = new CallLogObserver(mHandler, this, CallLogObserver.MSG_CALLLOG_QUERY_WHAT);
			getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver);//等价于【Uri.parse("content://call_log/calls")】
			break;
		case 4:
			if (smsContentObserver != null) getContentResolver().unregisterContentObserver(smsContentObserver);
			if (callLogObserver != null) getContentResolver().unregisterContentObserver(callLogObserver);
			break;
		case 5:
			registerReceiver(myReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
			break;
		case 6:
			try {
				unregisterReceiver(myReceiver);
			} catch (Exception e) {
			}
			break;
		}
	}

	/**
	 * 利用aidl及反射自动挂断来电。注意，不能通过ContentResolver监听通话记录数据库来挂断电话，估计是因为电话已接通，不能再挂掉了
	 */
	public void endCall() {
		//		IBinder iBinder = ServiceManager.getService(TELEPHONY_SERVICE);//希望调用的方法，但此方法被系统隐藏了
		try {
			Class<?> clazz = Class.forName("android.os.ServiceManager");//利用反射拿到其字节码文件
			Method method = clazz.getDeclaredMethod("getService", String.class);//获取ServiceManager类的getService(String s)方法
			IBinder ibinder = (IBinder) method.invoke(null, Context.TELEPHONY_SERVICE);//参数为：调用此方法的对象，此方法的参数
			ITelephony telephony = ITelephony.Stub.asInterface(ibinder);//把上面getService(String s)得到的IBinder对象转化成【ITelephony】对象
			boolean isSuccess = telephony.endCall();//调用ITelephony挂断电话的方法
			mHandler.sendMessage(Message.obtain(mHandler, 5, "是否成功挂断电话：" + isSuccess));
		} catch (Exception e) {
			mHandler.sendMessage(Message.obtain(mHandler, 5, "异常啦" + e.getMessage()));
			e.printStackTrace();
		}
	}

	/**监听来电状态的广播*/
	class PhoneStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				if (TelephonyManager.EXTRA_STATE_RINGING.equalsIgnoreCase(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {//来电状态
					endCall();
				}
			}
		}
	}
}