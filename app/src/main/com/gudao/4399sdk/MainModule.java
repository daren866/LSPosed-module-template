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
    
    // 目标游戏包名
    private static final String TARGET_PACKAGE = "com.joym.xiongdakuaipao";
    
    // 4399 SDK相关类名
    private static final String RECHARGE_LISTENER_CLASS = "cn.m4399.single.api.RechargeListener";
    private static final String SINGLE_GAME_CLASS = "cn.m4399.single.api.SingleGame";
    private static final String DELIVERING_LISTENER_CLASS = "cn.m4399.single.api.SingleGame$OnDeliveringGoodsListener";
    private static final String ORDER_CLASS = "cn.m4399.single.api.Order";
    private static final String ORDER_FINISHED_CLASS = "cn.m4399.single.api.OrderFinished";
    
    // UI相关
    private TextView statusText;
    private StringBuilder logBuilder = new StringBuilder();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建简单的UI
        statusText = new TextView(this);
        statusText.setTextSize(16);
        statusText.setPadding(50, 50, 50, 50);
        
        updateUI("4399内购助手 v1.0\n" +
                 "目标游戏: 兄弟快跑\n" +
                 "状态: 模块已加载\n" +
                 "请在LSPosed中勾选目标游戏\n\n" +
                 "日志输出:");
        
        setContentView(statusText);
        
        // 显示Toast提示
        Toast.makeText(this, "4399内购助手已激活", Toast.LENGTH_LONG).show();
    }
    
    /**
     * 更新UI显示
     */
    private void updateUI(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
    }
    
    /**
     * 添加日志到UI
     */
    private void addLog(final String log) {
        logBuilder.append(log).append("\n");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String currentText = statusText.getText().toString();
                if (currentText.contains("日志输出:")) {
                    statusText.setText(currentText + "\n" + log);
                } else {
                    statusText.setText(currentText + "\n\n日志输出:\n" + log);
                }
            }
        });
    }
    
    /**
     * IXposedHookLoadPackage 接口实现
     * 这是Xposed的入口点
     */
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只处理目标游戏
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }
        
        // 由于这是在Activity中，不能直接使用XposedBridge.log（可能跨线程问题）
        // 我们通过反射调用XposedBridge.log，或者用System.out临时替代
        try {
            logToXposed("=================================");
            logToXposed("4399内购模块已加载 - 目标游戏: " + TARGET_PACKAGE);
            logToXposed("=================================");
        } catch (Exception e) {
            // 忽略，可能没有Xposed环境
        }
        
        try {
            // Hook充值结果回调
            hookRechargeResult(lpparam);
            
            // Hook充值方法
            hookRechargeMethod(lpparam);
            
            // Hook客户端物品发放
            hookDeliveringGoods(lpparam);
            
            logToXposed("4399内购模块初始化完成");
        } catch (Exception e) {
            logToXposed("4399内购模块初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 安全的日志方法
     */
    private void logToXposed(String message) {
        try {
            XposedBridge.log(message);
        } catch (NoClassDefFoundError | Exception e) {
            // 在非Xposed环境（如Activity启动时）忽略
            System.out.println("XposedLog: " + message);
        }
    }
    
    /**
     * 1. Hook最重要的充值结果回调 - onRechargeFinished
     */
    private void hookRechargeResult(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> listenerClass = XposedHelpers.findClass(RECHARGE_LISTENER_CLASS, lpparam.classLoader);
            
            // Hook所有实现了RechargeListener的类的onRechargeFinished方法
            XposedBridge.hookAllMethods(listenerClass, "onRechargeFinished", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // 记录到Xposed日志
                    XposedBridge.log("=================== 支付回调被捕获 ===================");
                    
                    // 记录原始参数
                    for (int i = 0; i < param.args.length; i++) {
                        XposedBridge.log("参数[" + i + "]: " + param.args[i]);
                    }
                    
                    // 根据参数数量判断方法签名，强制设为成功
                    if (param.args.length >= 1) {
                        // 第一个参数是boolean success
                        param.args[0] = true;
                        XposedBridge.log("✅ 已强制设置 success = true");
                    }
                    
                    if (param.args.length >= 2) {
                        // 第二个参数可能是int resultCode或String msg
                        if (param.args[1] instanceof Integer) {
                            param.args[1] = 1;  // 设置为成功码
                            XposedBridge.log("✅ 已强制设置 resultCode = 1");
                        } else if (param.args[1] instanceof String) {
                            param.args[1] = "支付成功(Hook版)";
                            XposedBridge.log("✅ 已强制设置 msg = 支付成功(Hook版)");
                        }
                    }
                    
                    if (param.args.length >= 3) {
                        // 第三个参数通常是String msg
                        if (param.args[2] instanceof String) {
                            param.args[2] = "支付成功(Hook版)";
                            XposedBridge.log("✅ 已强制设置 msg = 支付成功(Hook版)");
                        }
                    }
                    
                    XposedBridge.log("=================== 回调修改完成 ===================");
                    
                    // 也添加到UI（但要注意线程）
                    addLog("💰 支付回调被Hook，强制成功");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 确保返回结果是true
                    if (param.getResult() instanceof Boolean) {
                        param.setResult(true);
                    }
                }
            });
            
            XposedBridge.log("✅ onRechargeFinished Hook成功");
            
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("❌ 未找到RechargeListener类，尝试替代方案");
            tryAlternativeRechargeListener(lpparam);
        } catch (Exception e) {
            XposedBridge.log("❌ hookRechargeResult 错误: " + e.getMessage());
        }
    }
    
    /**
     * 尝试其他可能的回调接口
     */
    private void tryAlternativeRechargeListener(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] possibleClasses = {
            "cn.m4399.single.api.OperateCenter$OnRechargeFinishedListener",
            "cn.m4399.single.api.RechargeCallback",
            "cn.m4399.single.api.PayListener",
            "cn.m4399.single.api.OnPayResultListener",
            "cn.m4399.single.api.OnRechargeListener",
            "com.joym.xiongdakuaipao.pay.PayResultListener" // 可能是游戏自定义的
        };
        
        for (String className : possibleClasses) {
            try {
                Class<?> listenerClass = XposedHelpers.findClass(className, lpparam.classLoader);
                
                // 尝试hook常见的方法名
                String[] possibleMethods = {"onRechargeFinished", "onPayResult", "onPayFinished", "onResult"};
                
                for (String methodName : possibleMethods) {
                    try {
                        XposedBridge.hookAllMethods(listenerClass, methodName, 
                            new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    // 强制成功逻辑
                                    if (param.args.length > 0) {
                                        if (param.args[0] instanceof Boolean) {
                                            param.args[0] = true;
                                        } else if (param.args[0] instanceof Integer) {
                                            // 假设0是成功
                                            param.args[0] = 0;
                                        }
                                    }
                                    XposedBridge.log("✅ 通过 " + className + "." + methodName + " 强制支付成功");
                                    addLog("✅ 通过替代方法强制成功");
                                }
                            });
                        XposedBridge.log("✅ 找到替代方法: " + className + "." + methodName);
                        return;
                    } catch (Exception e) {
                        // 继续尝试下一个方法名
                    }
                }
            } catch (XposedHelpers.ClassNotFoundError e) {
                // 继续尝试下一个
            }
        }
        
        XposedBridge.log("❌ 所有可能的回调类都未找到");
        addLog("❌ 未找到支付回调类，可能需要反编译游戏查看具体类名");
    }
    
    /**
     * 2. Hook充值方法，可以修改订单金额
     */
    private void hookRechargeMethod(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> singleGameClass = XposedHelpers.findClass(SINGLE_GAME_CLASS, lpparam.classLoader);
            
            // Hook recharge 方法
            XposedBridge.findAndHookMethod(singleGameClass, "recharge", 
                Activity.class, 
                XposedHelpers.findClass(ORDER_CLASS, lpparam.classLoader),
                XposedHelpers.findClass(RECHARGE_LISTENER_CLASS, lpparam.classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("💰 检测到充值请求");
                        addLog("💰 检测到充值请求");
                        
                        // 修改订单金额
                        if (param.args.length >= 2 && param.args[1] != null) {
                            Object order = param.args[1];
                            try {
                                // 尝试设置金额为0.01元
                                // 先尝试常见的字段名
                                String[] moneyFields = {"money", "amount", "price", "totalFee", "fee"};
                                
                                for (String field : moneyFields) {
                                    try {
                                        Object currentValue = XposedHelpers.getObjectField(order, field);
                                        if (currentValue instanceof String) {
                                            XposedHelpers.setObjectField(order, field, "0.01");
                                            XposedBridge.log("✅ 将 " + field + " 从 " + currentValue + " 改为 0.01");
                                        } else if (currentValue instanceof Integer) {
                                            XposedHelpers.setObjectField(order, field, 1);
                                            XposedBridge.log("✅ 将 " + field + " 从 " + currentValue + " 改为 1");
                                        } else if (currentValue instanceof Double) {
                                            XposedHelpers.setObjectField(order, field, 0.01);
                                            XposedBridge.log("✅ 将 " + field + " 从 " + currentValue + " 改为 0.01");
                                        }
                                    } catch (Exception e) {
                                        // 没有这个字段，继续尝试下一个
                                    }
                                }
                                
                                // 尝试设置商品数量为1
                                try {
                                    XposedHelpers.setObjectField(order, "count", 1);
                                } catch (Exception e) {
                                    // 忽略
                                }
                                
                            } catch (Exception e) {
                                XposedBridge.log("修改订单金额失败: " + e.getMessage());
                            }
                        }
                    }
                });
            
            XposedBridge.log("✅ recharge方法 Hook成功");
            
        } catch (Exception e) {
            XposedBridge.log("❌ recharge方法 Hook失败: " + e.getMessage());
            
            // 尝试其他可能的充值方法
            tryHookAlternativeRecharge(lpparam);
        }
    }
    
    /**
     * 尝试其他充值方法
     */
    private void tryHookAlternativeRecharge(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> singleGameClass = XposedHelpers.findClass(SINGLE_GAME_CLASS, lpparam.classLoader);
            
            // 尝试各种可能的充值方法签名
            String[][] methodSignatures = {
                {"doRecharge", Activity.class.getName(), ORDER_CLASS, "android.os.Handler"},
                {"pay", Activity.class.getName(), ORDER_CLASS},
                {"startPay", Activity.class.getName(), "java.lang.String", "java.lang.String"} // 商品ID, 金额
            };
            
            for (String[] sig : methodSignatures) {
                try {
                    XposedBridge.hookAllMethods(singleGameClass, sig[0], new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("💰 检测到替代充值方法: " + sig[0]);
                            addLog("💰 检测到充值方法: " + sig[0]);
                        }
                    });
                    XposedBridge.log("✅ 找到替代充值方法: " + sig[0]);
                    break;
                } catch (Exception e) {
                    // 继续尝试
                }
            }
        } catch (Exception e) {
            XposedBridge.log("❌ 所有充值方法Hook失败");
        }
    }
    
    /**
     * 3. Hook客户端物品发放回调
     */
    private void hookDeliveringGoods(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> deliveringListenerClass = XposedHelpers.findClass(
                DELIVERING_LISTENER_CLASS, lpparam.classLoader);
            
            XposedBridge.hookAllMethods(deliveringListenerClass, "onDelivering", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("📦 检测到物品发放回调");
                        addLog("📦 物品发放回调被触发");
                        
                        // 记录OrderFinished对象的信息
                        if (param.args.length > 0 && param.args[0] != null) {
                            Object orderFinished = param.args[0];
                            XposedBridge.log("订单信息: " + orderFinished.toString());
                            
                            // 尝试获取订单详情
                            try {
                                Object order = XposedHelpers.callMethod(orderFinished, "getOrder");
                                if (order != null) {
                                    XposedBridge.log("订单详情: " + order.toString());
                                }
                            } catch (Exception e) {
                                // 忽略
                            }
                        }
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // 确保返回true，表示物品发放成功
                        param.setResult(true);
                        XposedBridge.log("✅ 强制物品发放成功");
                        addLog("✅ 强制物品发放成功");
                    }
                });
            
            XposedBridge.log("✅ onDelivering Hook成功");
            
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("ℹ️ 未找到OnDeliveringGoodsListener（可能游戏没用这个）");
        } catch (Exception e) {
            XposedBridge.log("❌ onDelivering Hook失败: " + e.getMessage());
        }
    }
    
    /**
     * 辅助方法：打印类加载器中的所有类（用于调试）
     */
    private void dumpAllClasses(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.log("========== 开始扫描所有类 ==========");
            // 这个功能比较复杂，需要遍历Dex，这里只打印已知的4399相关类
            String[] keywords = {"4399", "pay", "recharge", "order", "sdk"};
            
            // 通过ClassLoader加载常见类来测试
            for (String keyword : keywords) {
                try {
                    // 尝试查找可能包含关键词的类
                    // 注意：这不能真正列出所有类，只是测试已知类是否存在
                    XposedBridge.log("检查关键词: " + keyword);
                } catch (Exception e) {
                    // 忽略
                }
            }
            XposedBridge.log("========== 扫描结束 ==========");
        } catch (Exception e) {
            XposedBridge.log("dumpClasses失败: " + e.getMessage());
        }
    }
}
