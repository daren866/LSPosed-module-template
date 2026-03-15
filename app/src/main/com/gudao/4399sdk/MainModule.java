package com.gudao.4399sdk;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainActivity extends Activity implements IXposedHookLoadPackage {
    
    // 目标游戏 - 熊出没之熊大快跑
    private static final String TARGET_PACKAGE = "com.joym.xiongdakuaipao";
    private static final String GAME_NAME = "熊出没之熊大快跑";
    
    // UI组件
    private TextView statusText;
    private StringBuilder logBuilder = new StringBuilder();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        statusText = new TextView(this);
        statusText.setTextSize(16);
        statusText.setPadding(50, 50, 50, 50);
        
        String welcomeMsg = "🐻 熊出没之熊大快跑 内购助手 v1.0 🐻\n" +
                           "=======================\n" +
                           "目标游戏: " + GAME_NAME + "\n" +
                           "包名: " + TARGET_PACKAGE + "\n" +
                           "状态: ✅ 模块已激活\n" +
                           "功能: 强制所有支付结果返回成功\n" +
                           "      (无论用户支付还是取消)\n" +
                           "=======================\n" +
                           "📱 实时日志:\n";
        
        updateUI(welcomeMsg);
        
        // 显示激活Toast
        Toast.makeText(this, "🐻 " + GAME_NAME + " 内购助手已激活", Toast.LENGTH_LONG).show();
        
        // 添加到日志
        addLog("✅ 模块启动成功");
        addLog("🎯 目标游戏: " + GAME_NAME);
        addLog("⚡ 等待游戏进程...");
    }
    
    private void updateUI(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
    }
    
    private void addLog(final String log) {
        logBuilder.append("▶ ").append(log).append("\n");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String currentText = statusText.getText().toString();
                if (currentText.contains("实时日志:")) {
                    // 保留前面的固定信息，只更新日志部分
                    String baseInfo = currentText.substring(0, currentText.indexOf("实时日志:") + 6);
                    statusText.setText(baseInfo + "\n" + logBuilder.toString());
                } else {
                    statusText.setText(currentText + "\n" + logBuilder.toString());
                }
            }
        });
    }
    
    private void logToXposed(String message) {
        try {
            XposedBridge.log("【" + GAME_NAME + "模块】" + message);
        } catch (NoClassDefFoundError | Exception e) {
            System.out.println("【" + GAME_NAME + "模块】" + message);
        }
    }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只处理目标游戏
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }
        
        // 打印醒目的激活日志
        logToXposed("==========================================");
        logToXposed("🐻🐻🐻 熊出没之熊大快跑 内购模块激活成功！🐻🐻🐻");
        logToXposed("==========================================");
        logToXposed("游戏包名: " + lpparam.packageName);
        logToXposed("进程名称: " + lpparam.processName);
        logToXposed("ClassLoader: " + lpparam.classLoader);
        logToXposed("==========================================");
        
        addLog("🔥 检测到游戏进程启动！");
        addLog("📦 开始注入Hook代码...");
        
        try {
            // 查找所有可能的4399支付相关类
            findAndHookAllPayClasses(lpparam);
            
            logToXposed("✅ 熊出没之熊大快跑 内购模块初始化完成！");
            logToXposed("==========================================");
            addLog("✅ 所有支付回调Hook完成！");
            addLog("💰 现在无论支付/取消都会返回成功");
            
        } catch (Exception e) {
            logToXposed("❌ 初始化失败: " + e.getMessage());
            addLog("❌ 错误: " + e.getMessage());
        }
    }
    
    /**
     * 查找并Hook所有可能的支付相关类
     */
    private void findAndHookAllPayClasses(XC_LoadPackage.LoadPackageParam lpparam) {
        // 可能的4399支付监听类列表
        String[] possibleListenerClasses = {
            "cn.m4399.single.api.RechargeListener",
            "cn.m4399.single.api.OperateCenter$OnRechargeFinishedListener",
            "cn.m4399.single.api.OnPayResultListener",
            "cn.m4399.single.api.PayListener",
            "cn.m4399.single.api.OnRechargeListener",
            "cn.m4399.single.api.IRechargeListener",
            "cn.m4399.single.api.PayResultListener",
            "cn.m4399.single.api.RechargeCallback",
            "com.joym.xiongdakuaipao.pay.PayResultListener",
            "com.joym.xiongdakuaipao.pay.OnPayListener",
            "com.joym.xiongdakuaipao.pay.RechargeListener"
        };
        
        // 所有可能的支付回调方法名（包含所有可能性）
        String[] allPossibleMethods = {
            // 完成类
            "onRechargeFinished",
            "onPayFinished",
            "onPayResult",
            "onResult",
            
            // 成功类
            "onRechargeSuccess",
            "onPaySuccess",
            "onSuccess",
            "onSucceed",
            "onComplete",
            
            // 失败类（我们要劫持成成功）
            "onRechargeFailed",
            "onPayFailed",
            "onFailure",
            "onFail",
            "onError",
            
            // 取消类（我们要劫持成成功）
            "onRechargeCanceled",
            "onPayCancel",
            "onCancel",
            "onCancelled",
            "onCanceled",
            
            // 其他
            "onCallback",
            "onResponse"
        };
        
        int hookedClasses = 0;
        int hookedMethods = 0;
        
        // 遍历所有可能的类
        for (String className : possibleListenerClasses) {
            try {
                Class<?> listenerClass = XposedHelpers.findClass(className, lpparam.classLoader);
                logToXposed("🔍 找到支付类: " + className);
                addLog("🔍 发现: " + className.substring(className.lastIndexOf('.') + 1));
                
                // 遍历所有可能的方法名
                for (String methodName : allPossibleMethods) {
                    try {
                        XposedBridge.hookAllMethods(listenerClass, methodName, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                String method = param.method.getName();
                                
                                // 打印详细的Hook信息
                                logToXposed("💰 [" + method + "] 被触发 - 强制改为成功");
                                
                                // 记录原始参数（用于调试）
                                StringBuilder argsInfo = new StringBuilder();
                                for (int i = 0; i < param.args.length; i++) {
                                    if (i > 0) argsInfo.append(", ");
                                    argsInfo.append(param.args[i]);
                                }
                                logToXposed("   原始参数: " + argsInfo.toString());
                                
                                // ===== 核心修改逻辑：强制所有参数为成功状态 =====
                                for (int i = 0; i < param.args.length; i++) {
                                    if (param.args[i] instanceof Boolean) {
                                        // 布尔参数：true = 成功
                                        boolean oldValue = (boolean) param.args[i];
                                        param.args[i] = true;
                                        logToXposed("   ✅ 布尔参数[" + i + "]: " + oldValue + " → true");
                                        
                                    } else if (param.args[i] instanceof Integer) {
                                        // 整数参数：0/1通常表示成功
                                        int oldValue = (int) param.args[i];
                                        // 尝试判断哪个值可能是成功码
                                        if (oldValue != 0 && oldValue != 1) {
                                            param.args[i] = 0;  // 大多数SDK用0表示成功
                                            logToXposed("   ✅ 整数参数[" + i + "]: " + oldValue + " → 0");
                                        } else {
                                            // 如果已经是0或1，确保是成功的那个
                                            param.args[i] = 0;
                                        }
                                        
                                    } else if (param.args[i] instanceof String) {
                                        // 字符串消息：改成成功消息
                                        String oldMsg = (String) param.args[i];
                                        param.args[i] = "支付成功（熊大快跑内购助手）";
                                        logToXposed("   ✅ 消息参数[" + i + "]: \"" + oldMsg + "\" → \"支付成功\"");
                                        
                                    } else if (param.args[i] != null) {
                                        // 其他对象类型，尝试查找其中的状态字段
                                        try {
                                            // 尝试设置常见的状态字段
                                            String[] statusFields = {"status", "code", "resultCode", "errorCode"};
                                            for (String field : statusFields) {
                                                try {
                                                    Object fieldValue = XposedHelpers.getObjectField(param.args[i], field);
                                                    if (fieldValue instanceof Integer) {
                                                        XposedHelpers.setIntField(param.args[i], field, 0);
                                                        logToXposed("   ✅ 对象字段[" + field + "]: " + fieldValue + " → 0");
                                                    } else if (fieldValue instanceof Boolean) {
                                                        XposedHelpers.setBooleanField(param.args[i], field, true);
                                                        logToXposed("   ✅ 对象字段[" + field + "]: " + fieldValue + " → true");
                                                    }
                                                } catch (Exception e) {
                                                    // 字段不存在，忽略
                                                }
                                            }
                                        } catch (Exception e) {
                                            // 忽略
                                        }
                                    }
                                }
                                
                                // 针对特定方法名的特殊处理
                                if (method.contains("Cancel") || method.contains("cancel") || 
                                    method.contains("Fail") || method.contains("fail") ||
                                    method.contains("Error") || method.contains("error")) {
                                    logToXposed("   ⚡ 这是取消/失败回调，已强制转为成功！");
                                }
                                
                                addLog("💰 " + method + " → 强制成功");
                            }
                            
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                // 处理返回值
                                if (param.getResult() != null) {
                                    if (param.getResult() instanceof Boolean) {
                                        boolean oldResult = (boolean) param.getResult();
                                        param.setResult(true);
                                        logToXposed("   ✅ 返回值: " + oldResult + " → true");
                                    } else if (param.getResult() instanceof Integer) {
                                        int oldResult = (int) param.getResult();
                                        param.setResult(0);
                                        logToXposed("   ✅ 返回值: " + oldResult + " → 0");
                                    }
                                }
                            }
                        });
                        hookedMethods++;
                        logToXposed("  ✅ Hook方法: " + methodName);
                        
                    } catch (Exception e) {
                        // 方法不存在，忽略
                    }
                }
                hookedClasses++;
                
            } catch (XposedHelpers.ClassNotFoundError e) {
                // 类不存在，继续尝试下一个
            }
        }
        
        logToXposed("📊 统计: 找到 " + hookedClasses + " 个支付类，Hook " + hookedMethods + " 个方法");
        
        if (hookedClasses == 0) {
            logToXposed("⚠️ 警告: 未找到任何4399支付类！尝试Hook游戏自定义类...");
            addLog("⚠️ 未找到标准4399类，尝试备用方案");
            hookGameCustomClasses(lpparam);
        }
    }
    
    /**
     * 备用方案：尝试Hook游戏可能自定义的支付类
     */
    private void hookGameCustomClasses(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] gameSpecificClasses = {
            "com.joym.xiongdakuaipao.PayManager",
            "com.joym.xiongdakuaipao.utils.PayUtil",
            "com.joym.xiongdakuaipao.activity.PayActivity",
            "com.joym.xiongdakuaipao.pay.PayHelper"
        };
        
        for (String className : gameSpecificClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
                logToXposed("🔍 找到游戏自定义类: " + className);
                addLog("🔍 发现游戏类: " + className.substring(className.lastIndexOf('.') + 1));
                
                // Hook常见的支付方法
                String[] payMethods = {"pay", "doPay", "recharge", "buy", "purchase"};
                for (String methodName : payMethods) {
                    try {
                        XposedBridge.hookAllMethods(clazz, methodName, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                logToXposed("💰 游戏支付方法被调用: " + param.method.getName());
                                addLog("💰 检测到支付请求");
                            }
                        });
                    } catch (Exception e) {
                        // 忽略
                    }
                }
                
            } catch (XposedHelpers.ClassNotFoundError e) {
                // 忽略
            }
        }
    }
}
