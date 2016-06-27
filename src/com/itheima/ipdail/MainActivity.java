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
		String[] array = { "ע��������ݿ�仯�Ĺ۲���", "�ռ������ݿ⡭��", "ɾ���������ͨ����¼", "����������ͨ����¼����ϸ��Ϣ", "ȡ��ע��Observer",//
				"ע��绰״̬�ı�Ĺ㲥����������ʱ�����Ҷϵ绰", "ȡ��ע��㲥", };
		for (int i = 0; i < array.length; i++) {
			array[i] = i + "��" + array[i];
		}
		ListAdapter mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(Arrays.asList(array)));
		tv_info = new TextView(this);// ��������ʾ��TextView��
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
			// boolean notifyForDescendents(����)����Ϊtrue�������������ָ����Uri��ͷ��Uri����Ϊfalse����ֻ��ȷ�ļ���ָ����URI
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
			getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver);//�ȼ��ڡ�Uri.parse("content://call_log/calls")��
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
	 * ����aidl�������Զ��Ҷ����硣ע�⣬����ͨ��ContentResolver����ͨ����¼���ݿ����Ҷϵ绰����������Ϊ�绰�ѽ�ͨ�������ٹҵ���
	 */
	public void endCall() {
		//		IBinder iBinder = ServiceManager.getService(TELEPHONY_SERVICE);//ϣ�����õķ��������˷�����ϵͳ������
		try {
			Class<?> clazz = Class.forName("android.os.ServiceManager");//���÷����õ����ֽ����ļ�
			Method method = clazz.getDeclaredMethod("getService", String.class);//��ȡServiceManager���getService(String s)����
			IBinder ibinder = (IBinder) method.invoke(null, Context.TELEPHONY_SERVICE);//����Ϊ�����ô˷����Ķ��󣬴˷����Ĳ���
			ITelephony telephony = ITelephony.Stub.asInterface(ibinder);//������getService(String s)�õ���IBinder����ת���ɡ�ITelephony������
			boolean isSuccess = telephony.endCall();//����ITelephony�Ҷϵ绰�ķ���
			mHandler.sendMessage(Message.obtain(mHandler, 5, "�Ƿ�ɹ��Ҷϵ绰��" + isSuccess));
		} catch (Exception e) {
			mHandler.sendMessage(Message.obtain(mHandler, 5, "�쳣��" + e.getMessage()));
			e.printStackTrace();
		}
	}

	/**��������״̬�Ĺ㲥*/
	class PhoneStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				if (TelephonyManager.EXTRA_STATE_RINGING.equalsIgnoreCase(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {//����״̬
					endCall();
				}
			}
		}
	}
}