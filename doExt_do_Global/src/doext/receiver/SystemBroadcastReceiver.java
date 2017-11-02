package doext.receiver;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import core.DoServiceContainer;
import core.interfaces.DoIGlobal;

public class SystemBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String _action = intent.getAction();
		JSONObject _node = new JSONObject();
		try {
			// 接收安装广播
			if ("android.intent.action.PACKAGE_ADDED".equals(_action)) {
				_node.put("type", "PACKAGE_ADDED");
				_node.put("content", intent.getDataString());
			}
			// 接收卸载广播
			if ("android.intent.action.PACKAGE_REMOVED".equals(_action)) {
				_node.put("type", "PACKAGE_REMOVED");
				_node.put("content", intent.getDataString());
			}
			// 接收开屏广播
			if (Intent.ACTION_SCREEN_ON.equals(_action)) {
				_node.put("type", 0);
			}
			// 接收锁屏广播
			if (Intent.ACTION_SCREEN_OFF.equals(_action)) {
				_node.put("type", 1);
			}
			// 接收解锁广播
			if (Intent.ACTION_USER_PRESENT.equals(_action)) {
				_node.put("type", 2);
			}
		} catch (Exception e) {
			try {
				_node.put("code", "1");
				_node.put("message", e.getMessage());
			} catch (Exception ex) {

			}
			DoServiceContainer.getLogEngine().writeError("SystemBroadcastReceiver onReceive \t\n\r", e);
		}
		DoIGlobal _global = DoServiceContainer.getGlobal();
		if (_global != null) {
			_global.fireEvent("broadcast", _node);
		}
	}

}
