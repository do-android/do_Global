package doext.implement;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoIApp;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
import doext.define.do_Global_IMethod;
import doext.receiver.SystemBroadcastReceiver;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_Global_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_Global_Model extends DoSingletonModule implements do_Global_IMethod {

	public do_Global_Model() throws Exception {
		super();
		this.dictApps = new HashMap<String, DoIApp>();
		this.dictMemCache = new HashMap<String, String>();
	}

	@Override
	public void dispose() {
		if (this.dictApps != null) {
			for (String _key : this.dictApps.keySet()) {
				this.dictApps.get(_key).dispose();
			}
			this.dictApps.clear();
			this.dictApps = null;
		}
		if (this.dictMemCache != null) {
			this.dictMemCache.clear();
			this.dictMemCache = null;
		}
	}

	private Map<String, DoIApp> dictApps;

	private String sourceRootPath;
	private String initDataRootPath;
	private String mappingSourceRootPath;
	private String dataRootPath;
	private int screenWidth;
	private int screenHeight;
	private int fullScreenWidth;
	private int fullScreenHeight;
	private String OSType;
	private String OSVersion;
	// 主应用ID
	private String mainAppID;
	private String scriptType;

	private int designScreenWidth;
	private int designScreenHeight;

	private Map<String, Boolean> allExitFile;

	@Override
	public String getSourceRootPath() {
		return this.sourceRootPath;
	}

	@Override
	public void setSourceRootPath(String _sourceRootPath) {
		this.sourceRootPath = _sourceRootPath;
	}

	@Override
	public String getMappingSourceRootPath() {
		return this.mappingSourceRootPath;
	}

	@Override
	public void setMappingSourceRootPath(String _mappingSourceRootPath) {
		this.mappingSourceRootPath = _mappingSourceRootPath;
	}

	@Override
	public String getDataRootPath() {
		return dataRootPath;
	}

	@Override
	public void setDataRootPath(String _dataRootPath) {
		this.dataRootPath = _dataRootPath;
	}

	@Override
	public String getInitDataRootPath() {
		return initDataRootPath;
	}

	@Override
	public void setInitDataRootPath(String _initDataRootPath) {
		this.initDataRootPath = _initDataRootPath;
	}

	public int getScreenWidth() {
		return screenWidth;
	}

	public int getScreenHeight() {
		return screenHeight;
	}

	@Override
	public void setScreenWidth(int _width) {
		this.screenWidth = _width;
	}

	@Override
	public void setScreenHeight(int _height) {
		this.screenHeight = _height;
	}

	@Override
	public void clearAllApps() {
		if (this.dictApps == null)
			return;
		this.dictApps.clear();
	}

	@Override
	public String getOSType() {
		return this.OSType;
	}

	@Override
	public String getOSVersion() {
		return this.OSVersion;
	}

	@Override
	public String getMainAppID() {
		return mainAppID;
	}

	@Override
	public String getScriptType() {
		return scriptType;
	}

	@Override
	public int getDesignScreenWidth() {
		return designScreenWidth;
	}

	@Override
	public int getDesignScreenHeight() {
		return designScreenHeight;
	}

	@Override
	public DoIApp getAppByID(String _appID) throws Exception {
		if (this.dictApps == null)
			return null;
		if (!this.dictApps.containsKey(_appID)) {
			String _appRootPath = this.getSourceRootPath() + "/" + _appID;
			if (!DoIOHelper.existDirectory(_appRootPath))
				return null;
			DoIApp _app = DoServiceContainer.getApp();
			_app.loadApp(_appID);
			this.dictApps.put(_appID, _app);
			_app.loadScripts();
		}
		return this.dictApps.get(_appID);
	}

	public void closeApp(String _appID) {
		if (this.dictApps == null)
			return;
		if (!this.dictApps.containsKey(_appID))
			return;
		this.dictApps.get(_appID).dispose();
		this.dictApps.remove(_appID);
	}

	@Override
	public DoIApp getAppByAddress(String _key) {
		return null;
	}

	@Override
	public void loadConfig(String _configFileName) throws Exception {
		if (!DoIOHelper.existFile(_configFileName))
			throw new Exception("不存在启动配置文件!");

		IntentFilter _filter = new IntentFilter();
		_filter.addAction(Intent.ACTION_SCREEN_OFF);
		_filter.addAction(Intent.ACTION_SCREEN_ON);
		_filter.addAction(Intent.ACTION_USER_PRESENT);
		_filter.setPriority(Integer.MAX_VALUE);
		DoServiceContainer.getPageViewFactory().getAppContext().registerReceiver(new SystemBroadcastReceiver(), _filter);

		String _configContent = DoIOHelper.readUTF8File(_configFileName);
		JSONObject baseJson = DoJsonHelper.loadDataFromText(_configContent).getJSONObject("Base");
		JSONObject designEnvironmentJson = DoJsonHelper.loadDataFromText(_configContent).getJSONObject("DesignEnvironment");
		this.mainAppID = DoJsonHelper.getString(baseJson, "AppID", null);
		this.scriptType = DoJsonHelper.getString(baseJson, "ScriptType", ".js");
		this.designScreenWidth = DoJsonHelper.getInt(designEnvironmentJson, "ScreenWidth", 750);
		this.designScreenHeight = DoJsonHelper.getInt(designEnvironmentJson, "ScreenHeight", 1334);
		if (null != this.scriptType && "lua".equals(this.scriptType)) {
			this.scriptType = ".lua";
		} else {
			this.scriptType = ".js";
		}
		if (this.mainAppID == null || this.mainAppID.length() <= 0)
			throw new Exception("启动配置文件中未设置主应用ID!");
	}

	// 处理成员方法
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("getTime".equals(_methodName)) { // 获取当前设备时间
			this.getTime(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("getWakeupID".equals(_methodName)) { // 获取整个应用唤醒id
			this.getWakeupID(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("getVersion".equals(_methodName)) { // 获取整个应用程序原生安装包的版本号
			this.getVersion(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("getMemory".equals(_methodName)) { // 获取全局的内存变量值
			this.getMemory(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("setMemory".equals(_methodName)) { // 设置全局的内存变量值
			this.setMemory(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("exit".equals(_methodName)) { // 退出应用
			this.exit(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("setToPasteboard".equals(_methodName)) { //
			this.setToPasteboard(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("getFromPasteboard".equals(_methodName)) { //
			this.getFromPasteboard(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("getSignatureInfo".equals(_methodName)) {
			this.getSignatureInfo(_dictParas, _scriptEngine, _invokeResult);
		}
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("install".equals(_methodName)) { // 安装新下载的zip文件，解压到到upgrade所在的目录
			this.install(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	// =====================================================================
	@Override
	public void install(JSONObject _dictParas, final DoIScriptEngine _scriptEngine, final String _callbackFuncName) throws Exception {
		String _installFile = DoJsonHelper.getString(_dictParas, "src", "");
		if (_installFile == null || _installFile.length() <= 0)
			throw new Exception("installfile无效!");
		final String _fullFileName = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _installFile);
		if (!DoIOHelper.existFile(_fullFileName))
			throw new Exception(_installFile + "不存在");
		String _upgradePath = new File(sourceRootPath).getParent() + "/upgrade";
		if (!DoIOHelper.existDirectory(_upgradePath)) {
			DoIOHelper.createDirectory(_upgradePath);
		}
		DoInvokeResult _invokeResult = new DoInvokeResult(this.getUniqueKey());
		try {
			DoIOHelper.unZipFolder(_fullFileName, _upgradePath);
			_invokeResult.setResultBoolean(true);
		} catch (IOException _err) {
			_invokeResult.setResultBoolean(false);
			DoServiceContainer.getLogEngine().writeError("instalfile 失败", _err);
		} finally {
			_scriptEngine.callback(_callbackFuncName, _invokeResult);
		}
	}

	@Override
	public void getTime(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		String _value = System.currentTimeMillis() + "";
		try {
			String _format = DoJsonHelper.getString(_dictParas, "format", null);
			if (_format != null && !"".equals(_format.trim())) {
				SimpleDateFormat sdf = new SimpleDateFormat(_format, Locale.getDefault());
				_value = sdf.format(new java.util.Date(Long.parseLong(_value)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			_invokeResult.setResultText(_value);
		}
	}

	@Override
	public void getWakeupID(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		int _actionid = _activity.getResources().getIdentifier("action_id", "string", _activity.getPackageName());
		_invokeResult.setResultText(_activity.getResources().getString(_actionid));
	}

	@Override
	public void exit(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		DoServiceContainer.getPageViewFactory().exitApp();
	}

	@Override
	public void getVersion(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		JSONObject _node = new JSONObject();
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		PackageManager _manager = _activity.getPackageManager();
		PackageInfo _info = _manager.getPackageInfo(_activity.getPackageName(), 0);
		_node.put("ver", _info.versionName);
		_node.put("code", _info.versionCode);
		_invokeResult.setResultNode(_node);
	}

	@Override
	public void getSignatureInfo(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		PackageManager _manager = _activity.getPackageManager();
		PackageInfo _info = _manager.getPackageInfo(_activity.getPackageName(), PackageManager.GET_SIGNATURES);

		Signature _signatures = _info.signatures[0];
		Signature[] sigs = _info.signatures; // 签名
		MessageDigest _md = MessageDigest.getInstance("MD5");
		_md.update(_signatures.toByteArray());
		byte[] _digest = _md.digest();
		String _md5 = toHexString(_digest);

		MessageDigest _md0 = MessageDigest.getInstance("SHA1");
		_md0.update(_signatures.toByteArray());
		byte[] _digest0 = _md0.digest();
		String _sha1 = toHexString(_digest0);

		MessageDigest _md1 = MessageDigest.getInstance("SHA256");
		_md1.update(_signatures.toByteArray());
		byte[] _digest1 = _md1.digest();
		String _sha256 = toHexString(_digest1);

		String[] _certMsg = new String[2];
		CertificateFactory _certFactory = CertificateFactory.getInstance("X.509");
		// 获取证书
		X509Certificate _cert = (X509Certificate) _certFactory.generateCertificate(new ByteArrayInputStream(sigs[0].toByteArray()));

		_certMsg[0] = _cert.getIssuerDN().toString();
		_certMsg[1] = _cert.getSubjectDN().toString();

		JSONObject _result = new JSONObject();

		_result.put("MD5", _md5);
		_result.put("SHA1", _sha1);
		_result.put("SHA256", _sha256);
		_result.put("version", _cert.getVersion());// 版本
		_result.put("sigAlgName", _cert.getSigAlgName());// 签名算法名称
		_result.put("notBefore", _cert.getNotBefore());// 有效期开始日期
		_result.put("notAfter", _cert.getNotAfter());// 截止日期
		_result.put("serialNumber", _cert.getSerialNumber().toString(16));// 序列号
		_result.put("issuerDN", _certMsg[0]);// 证书颁发者
		_result.put("subjectDN", _certMsg[1]);// 证书拥有者
		_invokeResult.setResultNode(_result);
	}

	private String toHexString(byte[] block) {
		StringBuffer buf = new StringBuffer();
		int len = block.length;
		for (int i = 0; i < len; i++) {
			byte2hex(block[i], buf);
			if (i < len - 1) {
				buf.append(":");
			}
		}
		return buf.toString();
	}

	private void byte2hex(byte b, StringBuffer buf) {
		char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		int high = ((b & 0xf0) >> 4);
		int low = (b & 0x0f);
		buf.append(hexChars[high]);
		buf.append(hexChars[low]);
	}

	private Map<String, String> dictMemCache;

	@Override
	public void getMemory(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _key = DoJsonHelper.getString(_dictParas, "key", null);
		if (_key != null && this.dictMemCache.containsKey(_key)) {
			_invokeResult.setResultText(this.dictMemCache.get(_key));
		} else {
			_invokeResult.setResultText("");
		}
	}

	@Override
	public void setMemory(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _key = DoJsonHelper.getString(_dictParas, "key", null);
		String _value = DoJsonHelper.getString(_dictParas, "value", null);
		this.dictMemCache.put(_key, _value);
	}

	@Override
	public void setToPasteboard(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		String _data = DoJsonHelper.getString(_dictParas, "data", "");
		try {
			ClipboardManager clipboardManager = (ClipboardManager) _activity.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboardManager.setPrimaryClip(ClipData.newPlainText(null, _data));
			_invokeResult.setResultBoolean(true);
		} catch (Exception e) {
			_invokeResult.setResultBoolean(false);
		}
	}

	@Override
	public void getFromPasteboard(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		ClipboardManager clipboardManager = (ClipboardManager) _activity.getSystemService(Context.CLIPBOARD_SERVICE);
		String _data = "";
		if (clipboardManager.hasPrimaryClip()) {
			_data = clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
		}
		_invokeResult.setResultText(_data);
	}

	@Override
	public void fireEvent(String _eventName, Object _result) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.getUniqueKey());
		if (_result != null) {
			if (_result instanceof JSONObject) {
				_invokeResult.setResultNode((JSONObject) _result);
			} else {
				_invokeResult.setResultText(_result.toString());
			}
		}
		this.getEventCenter().fireEvent(_eventName, _invokeResult);
	}

	@Override
	public int getFullScreenWidth() {
		return fullScreenWidth;
	}

	@Override
	public void setFullScreenWidth(int _width) {
		this.fullScreenWidth = _width;
	}

	@Override
	public int getFullScreenHeight() {
		return fullScreenHeight;
	}

	@Override
	public void setFullScreenHeight(int _height) {
		this.fullScreenHeight = _height;
	}

	@Override
	public void setAllExitFile(Map<String, Boolean> _allExitFile) {
		this.allExitFile = _allExitFile;
	}

	@Override
	public Map<String, Boolean> getAllExitFile() {
		return this.allExitFile;
	}
	
	@Override
	public String getMemoryValue(String _key) {
		if (this.dictMemCache.containsKey(_key)) {
			return this.dictMemCache.get(_key);
		}
		return null;
	}
	
	@Override
	public void clearMemory() {
		if (this.dictMemCache != null) {
			this.dictMemCache.clear();
		}
	}
}