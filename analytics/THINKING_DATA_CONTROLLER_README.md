# 数数SDK控制器使用说明

## 概述

`ThinkingDataController` 是数数SDK的封装控制器，提供了初始化、公共属性设置和数据上报功能。

## 功能特性

- ✅ **自动初始化**：在 `AnalyticsModuleProvider` 中自动初始化
- ✅ **屏蔽自动上报**：禁用数数SDK的自动事件上报
- ✅ **公共属性管理**：支持设置、获取、清除公共属性
- ✅ **事件上报**：支持自定义事件上报
- ✅ **双重上报**：同时上报到数数SDK和 `DataReportManager`
- ✅ **直接调用**：直接使用数数SDK API，性能更好
- ✅ **异常处理**：完善的异常处理和日志记录

## 配置要求

在 `analytics/build.gradle.kts` 中需要配置：

```kotlin
buildConfigField("String", "THINKING_DATA_APP_ID", "\"${analyticsConfig["thinkingDataAppId"]}\"")
buildConfigField("String", "THINKING_DATA_SERVER_URL", "\"${analyticsConfig["thinkingDataServerUrl"]}\"")
```

## 使用方法

### 1. 设置公共属性

```kotlin
// 设置单个公共属性
ThinkingDataController.setCommonProperty("user_id", "12345")
ThinkingDataController.setCommonProperty("user_type", "premium")

// 批量设置公共属性
val properties = mapOf(
    "app_version" to "1.0.0",
    "device_model" to "Pixel 6",
    "os_version" to "Android 13"
)
ThinkingDataController.setCommonProperties(properties)
```

### 2. 上报事件

```kotlin
// 上报简单事件
ThinkingDataController.trackEvent("button_click")

// 上报带属性的事件
val eventProperties = mapOf(
    "button_name" to "login",
    "page_name" to "home",
    "click_count" to 1
)
ThinkingDataController.trackEvent("button_click", eventProperties)
```

### 3. 获取公共属性

```kotlin
val commonProps = ThinkingDataController.getCommonProperties()
println("当前公共属性: $commonProps")
```

### 4. 清除公共属性

```kotlin
// 清除指定属性
ThinkingDataController.clearCommonProperty("user_id")

// 清除所有公共属性
ThinkingDataController.clearCommonProperty()
```

### 5. 检查初始化状态

```kotlin
if (ThinkingDataController.isInitialized()) {
    // SDK已初始化，可以正常使用
    ThinkingDataController.trackEvent("app_start")
} else {
    // SDK未初始化，等待初始化完成
}
```

## 集成示例

### 在Activity中使用

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置用户相关公共属性
        ThinkingDataController.setCommonProperty("user_id", getCurrentUserId())
        ThinkingDataController.setCommonProperty("user_level", getUserLevel())
        
        // 上报页面访问事件
        ThinkingDataController.trackEvent("page_view", mapOf(
            "page_name" to "main",
            "entry_point" to "launcher"
        ))
    }
    
    private fun onButtonClick(view: View) {
        // 上报按钮点击事件
        ThinkingDataController.trackEvent("button_click", mapOf(
            "button_id" to view.id.toString(),
            "button_text" to (view as? Button)?.text?.toString() ?: ""
        ))
    }
}
```

### 在业务逻辑中使用

```kotlin
class UserManager {
    fun login(userId: String, userType: String) {
        // 设置用户相关公共属性
        ThinkingDataController.setCommonProperty("user_id", userId)
        ThinkingDataController.setCommonProperty("user_type", userType)
        
        // 上报登录事件
        ThinkingDataController.trackEvent("user_login", mapOf(
            "login_method" to "email",
            "login_time" to System.currentTimeMillis()
        ))
    }
    
    fun logout() {
        // 上报登出事件
        ThinkingDataController.trackEvent("user_logout")
        
        // 清除用户相关公共属性
        ThinkingDataController.clearCommonProperty("user_id")
        ThinkingDataController.clearCommonProperty("user_type")
    }
}
```

## 日志输出

控制器会输出详细的日志信息：

```
D/AnalyticsLogger: 数数SDK初始化成功，App ID: your_app_id, Server URL: https://your-server.com
D/AnalyticsLogger: 数数SDK自动上报已屏蔽
D/AnalyticsLogger: 数数SDK默认公共属性设置完成
D/AnalyticsLogger: 数数SDK公共属性设置: user_id = 12345
D/AnalyticsLogger: 数数SDK事件上报: button_click, 属性: {user_id=12345, button_name=login, page_name=home}
```

## 注意事项

1. **依赖管理**：数数SDK依赖需要添加到项目中
2. **直接调用**：直接使用数数SDK API，性能更好
3. **双重上报**：事件会同时上报到数数SDK和 `DataReportManager`
4. **异常处理**：所有操作都有异常处理，不会影响应用正常运行
5. **线程安全**：使用 `ConcurrentHashMap` 保证线程安全

## 错误处理

如果数数SDK未正确配置或初始化失败，控制器会：

- 输出错误日志
- 确保应用不会崩溃
- 数据仍会上报到 `DataReportManager`
