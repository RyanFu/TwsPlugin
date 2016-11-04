package com.tws.plugin.core.systemservice;

import java.lang.reflect.Method;

import tws.component.log.TwsLog;

import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.manager.PluginManagerHelper;
import com.tws.plugin.core.proxy.MethodDelegate;
import com.tws.plugin.core.proxy.MethodProxy;

/**
 * @author yongchen
 */
public class SystemApiDelegate extends MethodDelegate {

	private static final String TAG = "BinderProxyDelegate";

	private final String descriptor;

	public SystemApiDelegate(String descriptor) {
		this.descriptor = descriptor;
	}

	public Object beforeInvoke(Object target, Method method, Object[] args) {
		TwsLog.d(TAG, "beforeInvoke:" + descriptor + " method:" + method.getName());

		// 这里做此判定是为了把一些特定的接口方法仍然交给特定的MethodProxy去处理,不在此做统一处理
		// 这些"特定的MethodProxy"主要是一些查询类接口
		// 单独判断checkPackage是因为AppOpsService的checkPackage方法会进入这里,
		// 而if里面的replacePackageName方法里
		// 面会触发一次ContentProvider调用,
		// ContentProvider调用又会触发AppOpsService的checkPackage方法,
		// AppOpsService的checkPackage方法被触发后又回进入这里,
		// 造成递归异常,因此这里单独屏蔽掉checkPackage方法
		if (!MethodProxy.sMethods.containsKey(method.getName()) && !"checkPackage".equals(method.getName())) {
			replacePackageName(args);
		}

		return null;
	}

	/**
	 * 由于插件的Context.getPackageName返回了插件自己的包名 这里需要在调用binder接口前将参数还原为宿主包名
	 * 
	 * @param args
	 */
	private void replacePackageName(Object[] args) {
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof String && ((String) args[i]).contains(".")) {
					// 包含.号,基本可以判定是packageName
					if (!args[i].equals(PluginLoader.getApplication().getPackageName())) {
						// 下面这个方法调用会触发ContentProvider
						PluginDescriptor pd = PluginManagerHelper.getPluginDescriptorByPluginId((String) args[i]);
						if (pd != null) {
							// 说明传的是插件包名, 修正为宿主包名
							args[i] = PluginLoader.getApplication().getPackageName();

							// 这里或许需要break,提高效率,
							// 因为一个接口的参数里面出现两个packageName的可能性较小
							// break;
						}
					}
				}
			}
		}
	}
}