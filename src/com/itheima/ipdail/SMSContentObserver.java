package com.itheima.ipdail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

/**�������ȡ�ֻ��������ݵ����ַ�ʽ
 * ��ʽһ��ͨ��ע��㲥��������
 * 				���ַ�ʽֻ�����յ��Ķ���Ϣ��Ч������ϵͳ������㲥������㲥��������һЩ���Ƶ�ϵͳ�����а�ȫ���������£���������Ϣ������ȡ���������ɵ���
 * ��������ͨ�������������ݿ�ı仯��ȡ����
 * 				���ַ�ʽ���Ի�ȡ�ֻ������еĶ��ţ������Ѷ�δ���Ķ��ţ����Ҳ��������������
 * ContentObserver��ʹ�����������ģʽ�еĹ۲���ģʽ��ContentObserver�ǹ۲��ߣ����۲��ContentProvider�Ǳ��۲��ߡ�
 * �����۲���ContentProvider�����ݷ�������ɾ�ĵı仯���ͻἰʱ��֪ͨ��ContentProvider��ContentObsserver������Ӧ�Ĵ���*/
public class SMSContentObserver extends ContentObserver {
	private Handler mHandler;
	private Context mContext;
	/**�۲����ͣ��������ݻ���ռ���*/
	private int observerType;
	/**�۲���������*/
	public static final int MSG_SMS_WHAT = 1;
	/**���۲��ռ���*/
	public static final int MSG_SMS_INBOX_WHAT = 2;

	public SMSContentObserver(Handler handler, Context context, int observerType) {
		super(handler);
		this.mHandler = handler;
		this.mContext = context;
		this.observerType = observerType;
	}

	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		if (observerType == MSG_SMS_WHAT) {
			Uri uri = Uri.parse("content://sms");
			Cursor cursor = mContext.getContentResolver().query(uri, new String[] { "_id", "address", "body", "type", "date" }, null, null, "date desc");
			if (cursor != null) {
				if (cursor.moveToFirst()) { //����յ��Ķ����ڵ�һ��. This method will return false if the cursor is empty
					int msgId = cursor.getInt(cursor.getColumnIndex("_id"));
					String msgAddr = cursor.getString(cursor.getColumnIndex("address"));
					String msgBody = cursor.getString(cursor.getColumnIndex("body"));
					String msgType = cursor.getString(cursor.getColumnIndex("type"));
					String msgDate = cursor.getString(cursor.getColumnIndex("date"));
					String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(Long.parseLong(msgDate)));
					String msgObj = "�ռ���\nId��" + msgId + "\n���룺" + msgAddr + "\n���ݣ�" + msgBody + "\n���ͣ�" + msgType + "\nʱ�䣺" + date + "\n";
					mHandler.sendMessage(Message.obtain(mHandler, MSG_SMS_WHAT, msgObj));
				}
				cursor.close();
			}
		} else if (observerType == MSG_SMS_INBOX_WHAT) {
			Uri uri = Uri.parse("content://sms/inbox");
			Cursor cursor = mContext.getContentResolver().query(uri, null, "read = 0", null, "date desc");//Passing null will return all columns, which is inefficient.
			//�ȼ��ڸ������� if (cursor.getInt(cursor.getColumnIndex("read")) == 0) //��ʾ����δ�������ַ�ʽ�����װ�������������ķ�ʽ��
			if (cursor != null) {
				StringBuilder sb = new StringBuilder("δ������\n");
				while (cursor.moveToNext()) {
					String sendNumber = cursor.getString(cursor.getColumnIndex("address"));
					String body = cursor.getString(cursor.getColumnIndex("body"));
					sb.append("���룺" + sendNumber + "\n���ݣ�" + body + "\n");
				}
				mHandler.obtainMessage(MSG_SMS_INBOX_WHAT, sb.toString()).sendToTarget();
				cursor.close();
			}
		}
	}
}