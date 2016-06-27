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

/**监听或获取手机短信内容的两种方式
 * 方式一：通过注册广播监听短信
 * 				这种方式只对新收到的短消息有效，并且系统的这个广播是有序广播，现在在一些定制的系统或是有安全软件的情况下，往往短消息都被截取到，并被干掉。
 * 方法二：通过监听短信数据库的变化获取短信
 * 				这种方式可以获取手机上所有的短信，包括已读未读的短信，并且不受其它程序干扰
 * ContentObserver的使用类似与设计模式中的观察者模式，ContentObserver是观察者，被观察的ContentProvider是被观察者。
 * 当被观察者ContentProvider的数据发生了增删改的变化，就会及时的通知给ContentProvider，ContentObsserver做出相应的处理。*/
public class SMSContentObserver extends ContentObserver {
	private Handler mHandler;
	private Context mContext;
	/**观察类型：所有内容或仅收件箱*/
	private int observerType;
	/**观察所有内容*/
	public static final int MSG_SMS_WHAT = 1;
	/**仅观察收件箱*/
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
				if (cursor.moveToFirst()) { //最后收到的短信在第一条. This method will return false if the cursor is empty
					int msgId = cursor.getInt(cursor.getColumnIndex("_id"));
					String msgAddr = cursor.getString(cursor.getColumnIndex("address"));
					String msgBody = cursor.getString(cursor.getColumnIndex("body"));
					String msgType = cursor.getString(cursor.getColumnIndex("type"));
					String msgDate = cursor.getString(cursor.getColumnIndex("date"));
					String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(Long.parseLong(msgDate)));
					String msgObj = "收件箱\nId：" + msgId + "\n号码：" + msgAddr + "\n内容：" + msgBody + "\n类型：" + msgType + "\n时间：" + date + "\n";
					mHandler.sendMessage(Message.obtain(mHandler, MSG_SMS_WHAT, msgObj));
				}
				cursor.close();
			}
		} else if (observerType == MSG_SMS_INBOX_WHAT) {
			Uri uri = Uri.parse("content://sms/inbox");
			Cursor cursor = mContext.getContentResolver().query(uri, null, "read = 0", null, "date desc");//Passing null will return all columns, which is inefficient.
			//等价于附加条件 if (cursor.getInt(cursor.getColumnIndex("read")) == 0) //表示短信未读。这种方式不靠谱啊，建议用上面的方式！
			if (cursor != null) {
				StringBuilder sb = new StringBuilder("未读短信\n");
				while (cursor.moveToNext()) {
					String sendNumber = cursor.getString(cursor.getColumnIndex("address"));
					String body = cursor.getString(cursor.getColumnIndex("body"));
					sb.append("号码：" + sendNumber + "\n内容：" + body + "\n");
				}
				mHandler.obtainMessage(MSG_SMS_INBOX_WHAT, sb.toString()).sendToTarget();
				cursor.close();
			}
		}
	}
}