package com.itheima.ipdail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;

/**
 * 拨号记录的内容观察者。
 */
public class CallLogObserver extends ContentObserver {
	/**观察到记录改变后的处理方式*/
	private int type;
	/**删除最近的一条通话记录*/
	public static final int MSG_CALLLOG_DELETE_WHAT = 3;
	/**查询某一个联系人最近的通话记录*/
	public static final int MSG_CALLLOG_QUERY_WHAT = 4;
	public static final String NUMBER = "17084143285";
	private Handler mHandler;
	private Uri uri = CallLog.Calls.CONTENT_URI;//等价于【Uri.parse("content://call_log/calls")】
	private ContentResolver resolver;

	public CallLogObserver(Handler handler, Context context, int type) {
		super(handler);
		this.mHandler = handler;
		this.type = type;
		resolver = context.getContentResolver();
	}

	@Override
	public void onChange(boolean selfChange) {
		Cursor cursor;
		switch (type) {
		case MSG_CALLLOG_DELETE_WHAT://删除最近的一条通话记录
			resolver.unregisterContentObserver(this);//注意：增删改通话记录后由于数据库发生变化，所以系统会在修改后再发一条广播，这时会重新回调onChange方法
			//最终导致的结果就是：一次来电后删除了多条甚至全部通话记录。为防止这种循环启发，必须在更改前就取消注册！事实上，注册的代码应该放在广播接收者中。
			cursor = resolver.query(uri, null, null, null, "_id desc limit 1");//按_id倒序排序后取第一个，即：查询结果按_id从大到小排序，然后取最上面一个（最近的通话记录）
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int num = resolver.delete(uri, "_id=?", new String[] { cursor.getInt(cursor.getColumnIndex("_id")) + "" });
					mHandler.sendMessage(Message.obtain(mHandler, MSG_CALLLOG_DELETE_WHAT, "删除的记录数量：" + num));
				}
				cursor.close();
			}
			break;
		case MSG_CALLLOG_QUERY_WHAT://查询某一个联系人最近的通话记录
			String[] projection = new String[] { "_id", CallLog.Calls.TYPE, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE, CallLog.Calls.DURATION };
			String selection = "number=? and (type=1 or type=3)";
			String[] selectionArgs = new String[] { NUMBER };
			String sortOrder = CallLog.Calls.DEFAULT_SORT_ORDER;//按时间排序【date DESC】
			cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int _id = cursor.getInt(cursor.getColumnIndex("_id"));
					int type = cursor.getInt(cursor.getColumnIndex("type"));//通话类型，1 来电 .INCOMING_TYPE；2 已拨 .OUTGOING_；3 未接 .MISSED_
					String number = cursor.getString(cursor.getColumnIndex("number"));// 电话号码
					String name = cursor.getString(cursor.getColumnIndex("name"));//联系人
					long date = cursor.getLong(cursor.getColumnIndex("date"));//通话时间，即可以用getString接收，也可以用getLong接收
					String formatDate = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(date));
					int duration = cursor.getInt(cursor.getColumnIndex("duration"));//通话时长，单位：秒

					String msgObj = "\nID：" + _id + "\n类型：" + type + "\n号码：" + number + "\n名称：" + name + "\n时间：" + formatDate + "\n时长：" + duration;
					mHandler.sendMessage(Message.obtain(mHandler, MSG_CALLLOG_QUERY_WHAT, msgObj));
				}
				cursor.close();
			}
			break;
		}
	}
}