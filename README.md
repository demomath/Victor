# 一、背景
随着业务的发展，APP在启动时需要加载一系列的组件，功能越多需要加载的组件就越多，而这些过程耗时都会占用APP的启动时间，所以APP冷启动耗时优化显得极为重要。

我们在做启动优化时，通常会将加载组件、初始化任务放到子线程或者IdleHandler中执行，但是实际操作时，场景往往是复杂的，比如
       1. B组件依赖于A组件，那么我们加载B组件时，必须首先要加载A组件，否则就会导致不可预估的异常发生；
       2. C模块可以独立进程管理，不需要在主进程初始化占用系统负载；
       .....
那么我们有没有可能提供一套启动任务框架解决以上问题？

如果有过相关了解的同学，应该知道Google Jetpack提供了App Startup，官方声明这是一个在Android应用启动时，针对初始化组件进行优化的依赖库。但是如果去翻看官方文档的话，其实只提到了可以通过一个ContentProvider来统一管理需要初始化的组件，然后通过dependencies() 方法解决组件间初始化的依赖顺序，对于异步处理，多模块相互依赖均未给出解决方案。那么没办法，还是只能自己思考如何解决。
# 二、需求分析
## 1. 需求点
首先分析我们的需求点，一个好的启动任务框架，必须具备以下功能：
* 能够优化启动耗时，支持多线程，多进程
* 业务调用足够简单，且不会增加耦合
* 支持业务的优先级
* 支持多任务依赖时，按照依赖关系依次执行       
## 2. 任务接口定义
要让业务调用足够简单，并且模块之间不能有耦合关系，我们采用自定义注解，通过APT解析后注入字节码文件实现。
通过以上需求分析我们任务的接口设计就显而易见。首先我们新建 java-library 项目，定义如下注解：
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface TaskAnnotation {
    /** 任务名称，需唯一 */
    String name();
    /** 是否在子线程执行 */
    boolean background() default true;
    /** 优先级，越小优先级越高 */
    int priority() default ITaskConstant.PRIORITY_NORM;
    /** 任务执行进程，支持主进程、非主进程、所有进程、:xxx、特定进程名 */
    String[] process() default {ITaskConstant.PROCESS_MAIN};
    /** 依赖的任务 */
    String[] depends() default {} ;
}
同时我们还需要一个执行任务的接口定义，新建 com.android.library 项目，创建接口：
public interface IRunTask extends ITask {
    /**
     * 执行任务
     * @param context 执行任务的上下文
     */
    void execute(Context context);
}
```
那么一个符合需求的任务就定义好了，使用示例如下：
```java
@TaskAnnotation(name = "TaskA", background = false, priority = 0, process = {ITaskConstant.PROCESS_MAIN}, depends = {
        "TaskB", "TaskC", "TaskD"})
public class TaskA implements IRunTask {
    @Override
    public void execute(Context context) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(IVictor.TAG, "任务A 对 「商城」 进行了初始化，耗时1s");
    }
}
```
# 三、依赖注入
## 1. 任务管理
有了接口定义，我们还需要一个任务管理类
```java
public class TaskRegisterManager {

    private ArrayList<TaskEntity> mTaskList = new ArrayList<>();

    /**
     * 静态内部类单例
     */
    private static class TaskRegisterManagerHolder {
        private static final TaskRegisterManager SINGLE_TON = new TaskRegisterManager();
    }
    private TaskRegisterManager() {
        init();
    }
    public static TaskRegisterManager getInstance() {
        return TaskRegisterManagerHolder.SINGLE_TON;
    }
    /**
     * TODO 得做点什么才能让任务添加进List
     */
    private void init() {

    }
    public ArrayList<TaskEntity> getTaskList() {
        return mTaskList;
    }
    private void register(ITaskRegister register) {
        register.register(mTaskList);
    }
}
```
## 2. 收集任务
接下来我们通过Annotation Processor收集任务，然后通过 javapoet 写入文件，那么再新建一个 java-library 项目，用作注解解析和写入文件，去生成帮忙将任务注册到集合的注册类。
```java
@AutoService(Processor.class)
public class TaskAnnotationProcessor extends AbstractProcessor {
    /**
     * 给出解析的注解范围
     *
     * @return Set<String>
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(TaskAnnotation.class.getCanonicalName());
    }

    /**
     * 解析注解后，并去生成 .java 文件
     *
     * @param annotations 注解
     * @param roundEnv    环境
     *
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(TaskAnnotation.class);
        if (elements == null || elements.size() == 0) {
            return false;
        }

        elementUtils.getTypeElement("com.android.task_interf.IRunTask");

        MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(ArrayList.class),
                                ClassName.get(TaskEntity.class)),
                        "taskList");
        for (Element element : elements) {
            TaskAnnotation taskAnnotation = element.getAnnotation(TaskAnnotation.class);
            System.out.println("taskAnnotation.name() = " + taskAnnotation.name());
            CodeBlock codeBlock = CodeBlock.builder()
                    .addStatement("taskList.add(new TaskEntity($S, $L, $L, $L, $L ,new $T()));",
                            taskAnnotation.name(), taskAnnotation.background(), taskAnnotation.priority(),
                            strarr2String(taskAnnotation.process()), strarr2String(taskAnnotation.depends()),
                            ClassName.get(element.asType()))
                    .build();
            builder.addCode(codeBlock);
        }
        MethodSpec registerMethod = builder.build();
        TypeSpec registerClass = TypeSpec.classBuilder("TaskRegister$" + moduleName)
                .addSuperinterface(ITaskRegister.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(registerMethod)
                .build();
        JavaFile javaFile = JavaFile.builder("com.android.task_register", registerClass).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    ....
}
```
编译后，在app/build/generated/ap_generated_sources/debug 会生成 .java 文件，看一下生成的文件长什么样
```java
public class TaskRegister$app implements ITaskRegister {
  @Override
  public void register(ArrayList<TaskEntity> taskList) {
    taskList.add(new TaskEntity("TaskA", true, 0, new String[]{"main"}, new String[]{} ,new TaskA()));
    taskList.add(new TaskEntity("TaskE", true, 1, new String[]{"main"}, new String[]{"TaskB", "TaskD"} ,new TaskE()));
  }
}
```
## 3. 注入字节码
可以看到收集到了一个任务，TaskEntity 则是任务的实体 Bean，除了拥有与注解定义的一致属性以外，还持有实际要执行的任务。
我们知道 APT 可以生成代码，但是无法修改字节码文件，也就是说我们在运行时，想拿到注入的任务，还需要将收集的任务注入到源码中。下面我们就来自定义 Gradle插件来完成这一操作。
新建 groovy 项目，并创建代码注入插件InjectPlugin
```java
public class InjectPlugin implements Plugin<Project> {
    public static final String EXT_NAME = 'inject_plugin'

    @Override
    public void apply(Project project) {
        // 注册transform接口
        def isApp = project.plugins.hasPlugin(AppPlugin)
        project.extensions.create(EXT_NAME, InjectPluginConfig)
        if (isApp) {
            println 'project(' + project.name + ') apply inject-plugin plugin'
            def android = project.extensions.getByType(AppExtension)
            def transformImpl = new InjectPluginTransform(project)
            android.registerTransform(transformImpl)
            project.afterEvaluate {
                init(project, transformImpl) // 此处要先于transformImpl.transform方法执行
            }
        }
    }

    static void init(Project project, InjectPluginTransform transformImpl) {
        InjectPluginConfig config = project.extensions.findByName(EXT_NAME) as InjectPluginConfig
        config.project = project
        config.convertConfig()
        transformImpl.config = config
    }
}
```
实际将生成的.java 代码写入字节码文件通过 InjectPluginTransform
```java
class InjectPluginTransform extends Transform {
    @Override
    void transform(Context context, Collection<TransformInput> inputs
                   , Collection<TransformInput> referencedInputs
                   , TransformOutputProvider outputProvider
                   , boolean isIncremental) throws IOException, TransformException, InterruptedException {
        project.logger.warn("start inject-plugin transform...")
        config.reset()
        project.logger.warn(config.toString())
        def clearCache = !isIncremental
        // clean build cache
        if (clearCache) {
            outputProvider.deleteAll()
        }
        long time = System.currentTimeMillis()
        boolean leftSlash = File.separator == '/'
        def cacheEnabled = config.cacheEnabled
        println("inject-plugin-----------isIncremental:${isIncremental}--------config.cacheEnabled:${cacheEnabled}--------------------\n")
        File jarManagerfile = null
        Map<String, ScanJarEntity> cacheMap = null
        File cacheFile = null
        Gson gson = null
        if (cacheEnabled) { // 开启了缓存
            gson = new Gson()
            cacheFile = InjectPluginHelper.getRegisterCacheFile(project)
            if (clearCache && cacheFile.exists())
                cacheFile.delete()
            cacheMap = InjectPluginHelper.readToMap(cacheFile, new TypeToken<HashMap<String, ScanJarEntity>>() {
            }.getType())
        }
        CodeScanProcessor scanProcessor = new CodeScanProcessor(config.list, cacheMap)
        // 遍历输入文件
        inputs.each { TransformInput input ->
            // 遍历jar
            input.jarInputs.each { JarInput jarInput ->
                if (jarInput.status != Status.NOTCHANGED && cacheMap) {
                    cacheMap.remove(jarInput.file.absolutePath)
                }
                scanJar(jarInput, outputProvider, scanProcessor)
            }
            // 遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->
                long dirTime = System.currentTimeMillis();
                // 获得产物的目录
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                String root = directoryInput.file.absolutePath
                if (!root.endsWith(File.separator))
                    root += File.separator
                // 遍历目录下的每个文件
                directoryInput.file.eachFileRecurse { File file ->
                    def path = file.absolutePath.replace(root, '')
                    if (file.isFile()) {
                        def entryName = path
                        if (!leftSlash) {
                            entryName = entryName.replaceAll("\\\\", "/")
                        }
                        scanProcessor.checkInitClass(entryName, new File(dest.absolutePath + File.separator + path))
                        if (scanProcessor.shouldProcessClass(entryName)) {
                            scanProcessor.scanClass(file)
                        }
                    }
                }
                long scanTime = System.currentTimeMillis();
                // 处理完后拷到目标文件
                FileUtils.copyDirectory(directoryInput.file, dest)
                println "inject-plugin cost time: ${System.currentTimeMillis() - dirTime}, scan time: ${scanTime - dirTime}. path=${root}"
            }
        }
        if (cacheMap != null && cacheFile && gson) {
            def json = gson.toJson(cacheMap)
            InjectPluginHelper.cacheRegisterHarvest(cacheFile, json)
        }
        def scanFinishTime = System.currentTimeMillis()
        project.logger.error("register scan all class cost time: " + (scanFinishTime - time) + " ms")
        config.list.each { ext ->
            if (ext.fileContainsInitClass) {
                println('')
                println("insert register code to file:" + ext.fileContainsInitClass.absolutePath)
                if (ext.classList.isEmpty()) {
                    project.logger.error("No class implements found for interface:" + ext.interfaceName)
                } else {
                    ext.classList.each {
                        println(it)
                    }
                    CodeInjectProcessor.insertInitCodeTo(ext)
                }
            } else {
                project.logger.error("The specified register class not found:" + ext.registerClassName)
            }
        }
        def finishTime = System.currentTimeMillis()
        project.logger.error("register insert code cost time: " + (finishTime - scanFinishTime) + " ms")
        project.logger.error("register cost time: " + (finishTime - time) + " ms")
    }
}
```
将我们写好的 Gradle 插件发布到本地，在 project 的 bulid.gradle 与 app 的 build.gradle 分别配置并引入插件
在 app 的 build.gradle 配置我们的注入信息
```java
inject_plugin {
    registerInfo = [
                            [
                                    'scanInterface'           : 'com.android.task_annotation.ITaskRegister'
                                    , 'codeInsertToClassName' : 'com.android.task_lib.TaskRegisterManager'
                                    , 'codeInsertToMethodName': 'init'
                                    , 'registerMethodName'    : 'register'
                                    , 'include'               : ["com/android/task_register/.*"]
                            ]
                    ]
    cacheEnabled = true
}
```
将收集到的任务注入到 init方法 中，执行编译，然后反编译apk 查看注入后的管理类字节码，这里可以与以上 TaskRegisterManager.java 类对比 init() 方法 发现我们的任务已经注入到了集合中。
```java
public class TaskRegisterManager {
    private ArrayList<TaskEntity> mTaskList;
    private TaskRegisterManager() {
        this.mTaskList = new ArrayList();
        this.init();
    }
    public static TaskRegisterManager getInstance() {
        return TaskRegisterManagerHolder.access$100();
    }
    private void init() {
        this.register(new TaskRegister$app());
        this.register(new TaskRegister$demo1());
    }
    public ArrayList<TaskEntity> getTaskList() {
        return this.mTaskList;
    }
    public void register(ITaskRegister var1) {
        var1.register(this.mTaskList);
    }
}
```
多写几个任务，运行打印日志验证一下：
```java
2022-09-01 16:33:30.873 23376-23376/com.android.task_demo E/victor: taskList.size = 4
```
可以看到目前任务采集已经完成了。
# 四、任务调度
## 1. 关系模型
在任务采集成功后，我们下一步就对任务进行调度。
实际业务中，模块任务之间往往因为模块依赖关系，导致任务之间存在依赖，首先我们先看几种常见的模块依赖关系。

通过观察我们可以发现，任意模块A所依赖的子模块是无法通过依赖传递继续依赖到自身，如果按照图论中的概念：一个有向图从任意顶点出发无法经过若干条边回到该点，则为有向无环图。
由此推断依赖关系图非常像有序无环图，其实事实也确实如此，处理依赖任务首先要构建一个有向无环图。
## 2. 执行顺序
首先，我们需要把任务分为两类，有依赖的任务和无依赖的任务。
* 有依赖任务
  * 判断有环，如果有循环依赖，直接 throw，这个可以套用公式 —— 如何判断有向图是否有环。
  * 判断无环，则收集每个任务的被依赖任务，我们称之为“子任务”，用于当前任务执行完成后，继续执行“子任务”。
* 无依赖任务
  * 直接按照优先级执行即可。
我们以下图来举例：

1. 分组梳理子任务
被依赖任务（子任务）
任务
依赖任务
无
A
B  C
A
B
C
A  B
C
D
A  C
D
无
1. 执行无依赖任务D
2. 更新已完成的任务D
被依赖任务（子任务）
任务
依赖任务
无
A
B  C
A
B
C
A B
C
无
1. 检查D的子任务是否可以执行，更新后的C可执行
2. 执行新的无依赖任务任务C
3. 重复步骤3-5，直到所有任务执行完成
3. 关系表示
以上流程中，被依赖任务我们称之为子任务，那么如何在有向无环图中找到一个任务的子任务呢？
首先可以看下图的一组任务依赖图

将关系放在二维数组relationalTable中：

举例：如图可知 relationalTable[C][] 为C的依赖项的数组，那么 relationalTable[][C] 则为C的子任务的数组，如此一来，我们想要找到每个执行完的任务的子任务就比较容易了。
4.代码实现
开始流程
```java
  public void start(Application application, RunTaskListener listener) {
    if (checkCircularDependency()) {
        throw new RuntimeException("启动任务存在循环依赖!");
    }
    this.mApplication = application;
    this.mListener = listener;
    // 去除task map不符合当前进程的任务
    removeOtherProcessTask();
    if (mTaskList.size() == 0) {
        releaseResources();
        return;
    }
    // 创建任务关系表
    createTaskRelationalTable();
    // 首批没有依赖的模块任务入队
    NoDependsTaskQueueOffer();
    mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TaskEntity entity = (TaskEntity) msg.obj;
            taskRunComplete(entity);
            luncherTask();
        }
    };
    // 任务从队列出队，执行任务
    luncherTask();
}
```
开始执行任务
```java
 /**
 * 开启执行
 */
private void luncherTask() {
    if (mNoDependencyQueue.size() == 0) {
        return;
    }
    TaskEntity entity = mNoDependencyQueue.poll();
    if (entity == null) {
        luncherTask();
        return;
    }
    if (entity.background) {
        Log.i(IVictor.TAG, "任务 " + entity.name + " 开启子线程开始执行");
        runTaskOnChildThread(entity);
    } else {
        Log.i(IVictor.TAG, "任务 " + entity.name + " 当前线程开始执行");
        runTaskOnUIThread(entity);
    }
}
```
任务完成

```java
 /**
 * 任务执行完成
 *
 * @param entity 任务
 */
private void taskRunComplete(TaskEntity entity) {
    if (mListener != null) {
        mListener.onSingleComplete(entity);
    }
    entity.executed = true;
    changeDependRelationalAndAddEntity2Queue(mTaskList.indexOf(entity));
    if (checkAllTaskRunComplete()) {
        if (mListener != null) {
            mListener.onAllComplete();
        }
        releaseResources();
    }
}
```
改变依赖关系，并将无依赖的任务实体进队
```java
 /**
 * 取消依赖关系
 *
 * @param i 被依赖的task index
 */
private void changeDependRelationalAndAddEntity2Queue(int i) {
    if (i < 0) {
        return;
    }
    for (int x = 0; x < mTaskRelationalTable.length; x++) {
        if (mTaskRelationalTable[x][i]) {
            boolean hasDepend = false;
            for (int y = 0; y < mTaskRelationalTable.length; y++) {
                if (y == i) {
                    mTaskRelationalTable[x][y] = false;
                } else {
                    hasDepend = hasDepend || mTaskRelationalTable[x][y];
                }
            }
            if (!hasDepend) {
                mNoDependencyQueue.offer(mTaskList.get(x));
            }
        }
    }
}
```
致此，整个任务调度执行流程完成，细心的同学可能还会发现，任务设置了优先级，那么如何保证的优先级呢？
首先TaskEntity 重写compareTo方法
```java
 public int compareTo(Object o) {
    if (o instanceof TaskEntity) {
        TaskEntity p = (TaskEntity) o;
        return p.priority - this.priority;
    }
    return 0;
}
```
然后执行队列采用了 PriorityQueue 结构，有兴趣的同学可以继续学习下。
# 五、验证结果
通过检查依赖，自动创建以下任务示例：
```java

tasks.add(
new 
TaskEntity(
"TaskA", true, 0, new 
String[]{
"main"
}
, new 
String[]{
"TaskB", "TaskC", "TaskD"
} 
,new 
TaskA()))
;

tasks.add(
new 
TaskEntity(
"TaskE", true, 0, new 
String[]{
"main"
}
, new 
String[]{} 
,new 
TaskE()))
;
tasks.add(new TaskEntity("TaskB", true, 0, new String[]{"main"}, new String[]{"TaskC"} ,new TaskB()));  
tasks.add(new TaskEntity("TaskC", true, 0, new String[]{"main"}, new String[]{"TaskD"} ,new TaskC()));;
tasks.add(new TaskEntity("TaskD", false, 0, new String[]{"main"}, new String[]{} ,new TaskD()));
```
打印执行结果
