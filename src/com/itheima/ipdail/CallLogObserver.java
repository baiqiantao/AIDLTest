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
 * ���ż�¼�����ݹ۲��ߡ�
 */
public class CallLogObserver extends ContentObserver {
	/**�۲쵽��¼�ı��Ĵ���ʽ*/
	private int type;
	/**ɾ�������һ��ͨ����¼*/
	public static final int MSG_CALLLOG_DELETE_WHAT = 3;
	/**��ѯĳһ����ϵ�������ͨ����¼*/
	public static final int MSG_CALLLOG_QUERY_WHAT = 4;
	public static final String NUMBER = "17084143285";
	private Handler mHandler;
	private Uri uri = CallLog.Calls.CONTENT_URI;//�ȼ��ڡ�Uri.parse("content://call_log/calls")��
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
		case MSG_CALLLOG_DELETE_WHAT://ɾ�������һ��ͨ����¼
			resolver.unregisterContentObserver(this);//ע�⣺��ɾ��ͨ����¼���������ݿⷢ���仯������ϵͳ�����޸ĺ��ٷ�һ���㲥����ʱ�����»ص�onChange����
			//���յ��µĽ�����ǣ�һ�������ɾ���˶�������ȫ��ͨ����¼��Ϊ��ֹ����ѭ�������������ڸ���ǰ��ȡ��ע�ᣡ��ʵ�ϣ�ע��Ĵ���Ӧ�÷��ڹ㲥�������С�
			cursor = resolver.query(uri, null, null, null, "_id desc limit 1");//��_id���������ȡ��һ����������ѯ�����_id�Ӵ�С����Ȼ��ȡ������һ���������ͨ����¼��
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int num = resolver.delete(uri, "_id=?", new String[] { cursor.getInt(cursor.getColumnIndex("_id")) + "" });
					mHandler.sendMessage(Message.obtain(mHandler, MSG_CALLLOG_DELETE_WHAT, "ɾ���ļ�¼������" + num));
				}
				cursor.close();
			}
			break;
		case MSG_CALLLOG_QUERY_WHAT://��ѯĳһ����ϵ�������ͨ����¼
			String[] projection = new String[] { "_id", CallLog.Calls.TYPE, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE, CallLog.Calls.DURATION };
			String selection = "number=? and (type=1 or type=3)";
			String[] selectionArgs = new String[] { NUMBER };
			String sortOrder = CallLog.Calls.DEFAULT_SORT_ORDER;//��ʱ������date DESC��
			cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int _id = cursor.getInt(cursor.getColumnIndex("_id"));
					int type = cursor.getInt(cursor.getColumnIndex("type"));//ͨ�����ͣ�1 ���� .INCOMING_TYPE��2 �Ѳ� .OUTGOING_��3 δ�� .MISSED_
					String number = cursor.getString(cursor.getColumnIndex("number"));// �绰����
					String name = cursor.getString(cursor.getColumnIndex("name"));//��ϵ��
					long date = cursor.getLong(cursor.getColumnIndex("date"));//ͨ��ʱ�䣬��������getString���գ�Ҳ������getLong����
					String formatDate = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(date));
					int duration = cursor.getInt(cursor.getColumnIndex("duration"));//ͨ��ʱ������λ����

					String msgObj = "\nID��" + _id + "\n���ͣ�" + type + "\n���룺" + number + "\n���ƣ�" + name + "\nʱ�䣺" + formatDate + "\nʱ����" + duration;
					mHandler.sendMessage(Message.obtain(mHandler, MSG_CALLLOG_QUERY_WHAT, msgObj));
				}
				cursor.close();
			}
			break;
		}
	}
}