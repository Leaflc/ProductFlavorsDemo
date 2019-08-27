# Gradle配置编译变体实现多渠道打包差异化

### 概述

在开发的过程中可能会遇到一种场景，就是在不同应用商店上架的应用名，应用图标，用的推送平台等不一样。这个时候可能很多人都会针对一些应用商店临时修改打包，打包完后再改回去。这样一旦这种差异量越多，版本发的多。很不好维护，经常会出现漏改，或者忘改回去的尴尬情况。所以这个时候就需要用到了gradle 的product flavor实现这些打包差异化。

### 实际效果
先看效果


## 配置版本类型
您可以在 android 块内的模块级 build.gradle 文件中创建和配置版本类型。在我们创建一个Android项目的时候，一般AndroidStudio会自动帮我们创建"debug"和release这个两个版本类型。一般debug版本类型并没有显式的显示在build.gradle配置文件中。就像下面这样
```
   buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
```
但是，实际是有配置的，通过以下步骤就可以看到

<img src="https://user-gold-cdn.xitu.io/2019/8/27/16cd1ee17a3e7ea6?w=970&h=1102&f=png&s=178713" width="400" align=center />

虽然“debug”版本类型没有显示在编译配置文件中，但 Android Studio 会使用 debuggable true 配置它。这样，您就可以在安全的 Android 设备上调试应用，并使用常规调试密钥库配置 APK 签名。

如果要添加或更改某些设置，则可以将“debug”版本类型添加到您的配置中。以下示例为调试版本类型指定了 applicationIdSuffix，并配置了一个使用“debug”版本类型的设置进行初始化的“staging”版本类型
```
    android {
        defaultConfig {
            manifestPlaceholders = [hostName:"www.example.com"]
            ...
        }
        buildTypes {
            release {
                minifyEnabled true
                proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            }

            debug {
                applicationIdSuffix ".debug"
                debuggable true
            }

            /**
             * initwidth 属性允许您复制其他构建类型配置，这里复制了debug
             * 然后仅配置要更改的设置。这里更改清单占位符和应用程序ID。
             */
            staging {
                initWith debug
                manifestPlaceholders = [hostName:"internal.example.com"]
                /**
                 * 这里修改ApplicationId后缀,假设gradle配置的applicationId是com.test的话
                 * 那当你切换到这个构建类型的话，打包应用的话，实际applicationId是com.test.debugStaging
                 */
                applicationIdSuffix ".debugStaging"
            }
        }
    }

```

我们这里同步下，于是

<img src="https://user-gold-cdn.xitu.io/2019/8/27/16cd1fa0527185b8?w=648&h=842&f=png&s=86942" width="400" align=center /> <br>

这里可以看到刚刚我们添加的构建类型，这里点击下就可以直接切换了

## 配置产品特性
创建产品特性与创建版本类型类似：将其添加到编译配置中的 productFlavors 代码块并添加所需的设置。
所有类型都必须属于一个指定的flavorDimensions (类型维度)，即一个产品特性组。即使您打算只使用一个维度，也必须将类型分配到类型维度，否则您会收到以下编译错误：
```
    Error:All flavors must now belong to a named flavor dimension.
    The flavor 'flavor_name' is not assigned to a flavor dimension.
```
以下代码示例创建了一个名为“version”的类型维度，并添加了“demo”和“full”产品特性。这些类型提供了它们自己的 applicationIdSuffix 和 versionNameSuffix：
```
    android {
        ...
        defaultConfig {...}
        buildTypes {
            debug{...}
            release{...}
        }
        // 指定一个风格特性组
        flavorDimensions "version"
        productFlavors {
            demo {
                // 使当前风格归属于version特性组
                dimension "version"
                // 添加applicationId后缀
                applicationIdSuffix ".demo"
                // 添加版本名后缀
                versionNameSuffix "-demo"
            }
            full {
                dimension "version"
                // 添加applicationId后缀
                applicationIdSuffix ".full"
                // 添加版本名后缀
                versionNameSuffix "-full"
            }
        }
    }

```

创建并配置productFlavor后，点击通知栏中的 Sync Now。同步完成后，Gradle 会根据您的版本类型和产品特性自动创建编译变体，并根据 <product-flavor><Build-Type> 对其进行命名。例如，如果您创建了“demo”和“full”产品特性，并保留默认的“debug”和“release”版本类型，则 Gradle 会创建以下编译变体：
* *demoDebug*
* *demoRelease*
* *fullDebug*
* *fullRelease*

您可以将编译变体更改为您要编译并运行的任意变体，只需依次转到 Build > Select Build Variant，然后从下拉菜单中选择一种变体即可。

### 将多个产品特性与类型维度结合使用

假设以下场景，就是需要打包两个不同的推送平台的应用，且在不同渠道也有应用名不同的差异，这个时候就需要合并多个产品特性的配置。

以下代码示例使用 flavorDimensions 属性来创建“push_channe”推送渠道和“channel”应用上架渠道两个特性维度，
前者用于将“jpush”,"xiaomi"这个产品特性分组，后者将“vivo,huawei”产品特性进行分组
```
   android {
      ...
      buildTypes {
        debug {...}
        release {...}
      }


      flavorDimensions "push_channel", "channel"

      productFlavors {
        // 极光推送
        jpush {
          dimension "push_channel"
          ...
        }

       // 小米推送
        xiaomi {
          dimension "push_channel"
          ...
        }

       // vivo应用商店
        vivo {
          dimension "channel"
          minSdkVersion 24
          versionCode 30000
          versionNameSuffix "-minApi24"
          ...
        }

       // 华为应用商店
        huawei {
          dimension "channel"
          minSdkVersion 23
          versionCode 20000
          versionNameSuffix "-minApi23"
          ...
        }
      }
    }
    ...

```

Gradle 创建的编译变体数量等于每个类型维度中的类型数与您配置的版本类型数量的乘积。当 Gradle 为每个编译变体或相应的 APK 命名时，属于较高优先级类型维度的产品特性会先显示，然后是属于较低优先级维度的产品特性，然后是版本类型。以上面的编译配置为例，Gradle 使用以下命名方案创建了总共 8个编译变体：

![](https://user-gold-cdn.xitu.io/2019/8/27/16cd21146bd4c329?w=412&h=478&f=png&s=74317)


## 常见差异实现

### 修改strings中某个string name的值
例如修改
```
    <string name="app_name">ConfigureBuildVariantsDemo</string>
```
可以这样
```

  productFlavors {


        vivo {
            resValue("string", "app_name", "测试")
            dimension "channel"
        }

        xiaomi{
             resValue("string", "app_name", "测试2")
             dimension "channel"
        }

        }
```

### 修改manifest文件 的meta-data值

关于[meta-data的应用](https://blog.csdn.net/janice0529/article/details/41583587址 "meta-data应用")   <br>
AndroidManifest.xml
```
    <meta-data
            android:name="BUGLY_APPID"
            android:value="${BUGLY_APPID}" />

    <meta-data
            android:name="BASE_URL"
            android:value="${BASE_URL}" />

    // 注意这里有个小坑就是如果你在gradle文件配置的字符串变量是01或者023开头的话，
    // 在你从java/kotlin代码获取这个值的时候，他会把他当作一个数字，格式化成1，23，
    // 所以在这里结尾加了这个0，在你从代码中记得删除结尾这个0
     <meta-data
            android:name="PASSWORD"
            android:value="${PASSWORD}\0" />
```



```
  productFlavors {


        vivo {
            resValue("string", "app_name", "测试")
            dimension "channel"
                 manifestPlaceholders = [
                                    BASE_URL             : "https://www.vivo.com.cn/",
                                    BUGLY_APPID          : "0c01e82354"]
        }

        xiaomi{
             resValue("string", "app_name", "测试2")
             dimension "channel"
                manifestPlaceholders = [
                                    BASE_URL             : "http://www.xiaomi.com/",
                                    BUGLY_APPID          : "0c01e82334"]
        }

    }
```

### 不同的资源文件
假设你需要根据不同的渠道设置不同的应用图标，不同的布局文件你可以这样
```
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']
            assets.srcDirs = ['src/main/assets', 'src/main/assets/']
            res.srcDirs = ['src/main/res', 'src/main/res/']
        }

       // 这里要指定productFlavor下有配置的特性，不然sync now会报错
       vivo {
            res.srcDirs = ['src/vivo/res', 'src/vivo/res/']
        }

        xiaomi {
            res.srcDirs = ['src/xiaomi/res', 'src/xiaomi/res/']
        }

    }

        flavorDimensions  "channel"


      productFlavors {


        vivo {
            resValue("string", "app_name", "测试")
            dimension "channel"
                 manifestPlaceholders = [
                                    BASE_URL             : "https://www.vivo.com.cn/",
                                    BUGLY_APPID          : "0c01e82354"]
        }

        xiaomi{
             resValue("string", "app_name", "测试2")
             dimension "channel"
                manifestPlaceholders = [
                                    BASE_URL             : "http://www.xiaomi.com/",
                                    BUGLY_APPID          : "0c01e82334"]
        }

    }
```

然后

![](https://user-gold-cdn.xitu.io/2019/8/27/16cd226284675fff?w=1812&h=1300&f=png&s=564200)

点击AndroidResourceDirectory新建资源文件路径


![](https://user-gold-cdn.xitu.io/2019/8/27/16cd229db919c55a?w=1700&h=966&f=png&s=236981)
然后选择对应风格新建对应的文件夹，然后把对应的不一样的资源文件往里面放进去

## Demo
github地址 ：https://github.com/Leaflc/ProductFlavorsDemo

## 引用
[Android Developers](https://user-gold-cdn.xitu.io/2019/8/27/16cd22bd09f9c701)