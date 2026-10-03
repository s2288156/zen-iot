import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.Properties

// 本机开发栈编排：devUp 起中间件 + 三个后端 + 前端，devDown 只停自己拉起的进程，devStatus 报状态。
// 这三个任务不进任何门禁，也不被 check / prePushCheck 依赖，所以不违背 docs/modules/web.md 的
// 「前端不进 Gradle」——那条决策约束的是工具链与门禁语义，这里只是拉起一个本机进程。

val devRootDir = layout.buildDirectory.dir("dev")
val devPidDir = devRootDir.map { it.dir("pids").asFile }
val devLogDir = devRootDir.map { it.dir("logs").asFile }

// 根脚本的同名变量是脚本局部值，跨脚本不可见，这里自带一份
val isWindows = System.getProperty("os.name").lowercase().contains("windows")

// 启动顺序：业务服务在前，网关最后（它靠 lb:// 懒解析下游，先起不依赖实例），前端依赖网关端口已就绪
val devBootModules = listOf("admin-service", "ecs-service", "gateway-service")
val devWebName = "web"
val devComposeFile = File(rootDir, "docker/docker-compose.dev.yml")

// Nacos 冷启动 + Flyway 灌库是这里的大头，给 30s 会误报超时
val devMiddlewareTimeout = devIntProperty("devMiddlewareTimeout", 120)
val devServiceTimeout = devIntProperty("devServiceTimeout", 150)
val devWebTimeout = devIntProperty("devWebTimeout", 40)

fun devIntProperty(name: String, fallback: Int): Int =
	project.findProperty(name)?.toString()?.toIntOrNull() ?: fallback

// 端口只在各服务自己的配置里声明一次，这里读回来用，不另建一张端口表
fun readServerPort(module: String): Int {
	val yml = File(rootDir, "$module/src/main/resources/application.yml")
	var inServer = false
	yml.readLines().forEach { line ->
		if (line.trim() == "server:") {
			inServer = true
		} else if (inServer) {
			val match = Regex("""^\s+port:\s*(\d+)\s*$""").find(line)
			if (match != null) return match.groupValues[1].toInt()
			if (line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("\t")) inServer = false
		}
	}
	throw GradleException("$yml 里没有 server.port")
}

fun readVitePort(): Int {
	val ts = File(rootDir, "$devWebName/vite.config.ts")
	return Regex("""\bport:\s*(\d+)""").find(ts.readText())?.groupValues?.get(1)?.toInt()
		?: throw GradleException("$ts 的 server 块里没有 port：dev 需要固定端口，否则探测会跟着 Vite 的自动换端漂走")
}

fun runCapture(args: List<String>): Pair<Int, String> {
	val process = try {
		ProcessBuilder(args).redirectErrorStream(true).start()
	} catch (e: Exception) {
		throw GradleException("命令执行失败：${args.joinToString(" ")}（${e.message}）", e)
	}
	val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
	return process.waitFor() to output
}

class DevTarget(
	val name: String,
	val port: Int,
	val readinessPath: String,
	val waitSeconds: Int,
	val command: () -> List<String>,
	val workingDir: () -> File,
) {
	val pidFile: File = File(devPidDir.get(), "$name.pid")
	val logFile: File = File(devLogDir.get(), "$name.log")
}

fun bootTarget(module: String) = DevTarget(
	name = module,
	port = readServerPort(module),
	readinessPath = "/actuator/health",
	waitSeconds = devServiceTimeout,
	command = {
		val spec = Properties().apply {
			val file = File(rootDir, "$module/build/dev/spec.properties")
			if (!file.isFile) throw GradleException("缺少 $file，先跑 ./gradlew :$module:devSpec")
			file.inputStream().use { load(it.reader(Charsets.UTF_8)) }
		}
		val args = File(rootDir, "$module/build/dev/java.args")
		if (!args.isFile) throw GradleException("缺少 $args，先跑 ./gradlew :$module:devSpec")
		listOf(spec.getProperty("java"), "@${args.absolutePath}")
	},
	workingDir = { File(rootDir, module) },
)

fun webTarget() = DevTarget(
	name = devWebName,
	port = readVitePort(),
	readinessPath = "/",
	waitSeconds = devWebTimeout,
	// 直接 node + vite.js，不经 pnpm.cmd：单进程 PID 干净、不需要 shell；--strictPort 让端口被占时显式失败
	command = {
		val entry = File(rootDir, "$devWebName/node_modules/vite/bin/vite.js")
		if (!entry.isFile) throw GradleException("未找到 $entry，先在 web/ 跑 pnpm install")
		listOf("node", entry.absolutePath, "--port", readVitePort().toString(), "--strictPort")
	},
	workingDir = { File(rootDir, devWebName) },
)

fun devTargets(): List<DevTarget> = devBootModules.map { bootTarget(it) } + webTarget()

fun pidAlive(pid: Long): Boolean = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

fun pidFileValue(target: DevTarget): Long? =
	target.pidFile.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull()

fun tcpUp(host: String, port: Int): Boolean = try {
	Socket().use { it.connect(InetSocketAddress(host, port), 300) }
	true
} catch (e: Exception) {
	false
}

// 回环地址逐个试：Vite 8 默认只绑 [::1]:5173（实测），而 JDK 的 localhost 优先解析到 IPv4，
// 只探一个族会让就绪判定永远失败
val devLoopbackHosts = listOf("127.0.0.1", "::1", "localhost")

fun portListening(port: Int): Boolean = devLoopbackHosts.any { tcpUp(it, port) }

// 端口占用者只用于报错，让开发者自己决定杀不杀：devUp 绝不碰陌生进程
fun portOwner(port: Int): String {
	val pid = if (isWindows) {
		runCapture(listOf("netstat", "-ano")).second.lineSequence()
			.filter { it.contains("LISTENING") }
			.map { it.trim().split(Regex("\\s+")) }
			.firstOrNull { it.size >= 5 && it[1].endsWith(":$port") }
			?.last()?.toLongOrNull()
	} else {
		Regex("""pid=(\d+)""").find(runCapture(listOf("sh", "-c", "ss -ltnp 'sport = :$port'")).second)
			?.groupValues?.get(1)?.toLongOrNull()
	} ?: return "未知"
	return "PID $pid（${ProcessHandle.of(pid).flatMap { h -> h.info().command() }.orElse("命令未知")}）"
}

fun ready(target: DevTarget): Boolean = devLoopbackHosts.any { host ->
	try {
		val url = "http://" + (if (host.contains(":")) "[$host]" else host) + ":${target.port}${target.readinessPath}"
		val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
		connection.connectTimeout = 500
		connection.readTimeout = 1500
		val code = connection.responseCode
		connection.disconnect()
		code in 200..399
	} catch (e: Exception) {
		false
	}
}

fun awaitReady(target: DevTarget) {
	val deadline = System.currentTimeMillis() + target.waitSeconds * 1000L
	var tick = 0L
	while (System.currentTimeMillis() < deadline) {
		if (ready(target)) {
			logger.lifecycle("  READY   {} 端口 {}", target.name, target.port)
			return
		}
		if (System.currentTimeMillis() - tick > 5000) {
			logger.lifecycle("  waiting {} ...", target.name)
			tick = System.currentTimeMillis()
		}
		Thread.sleep(500)
	}
	val tail = target.logFile.takeIf { it.isFile }?.readLines()?.takeLast(20)?.joinToString("\n") ?: "（还没有日志）"
	throw GradleException(
		"${target.name} 在 ${target.waitSeconds}s 内没有就绪（端口 ${target.port}）。日志尾部：\n$tail\n" +
			"完整日志：${target.logFile}",
	)
}

fun spawn(target: DevTarget) {
	target.logFile.parentFile.mkdirs()
	val builder = ProcessBuilder(target.command())
		.directory(target.workingDir())
		.redirectOutput(target.logFile)
		.redirectErrorStream(true)
	val process = try {
		builder.start()
	} catch (e: Exception) {
		throw GradleException("启动 ${target.name} 失败：${e.message}", e)
	}
	target.pidFile.writeText(process.pid().toString())
}

fun killTree(pid: Long) {
	if (isWindows) {
		// Windows 没有 SIGTERM：taskkill /F 不走优雅停机，Spring 的 shutdown hook 不执行，dev 场景接受这个代价
		runCapture(listOf("taskkill", "/PID", pid.toString(), "/T", "/F"))
	} else {
		ProcessHandle.of(pid).ifPresent { it.destroyForcibly() }
	}
}

fun startTarget(target: DevTarget) {
	val pid = pidFileValue(target)
	if (portListening(target.port)) {
		if (pid != null && pidAlive(pid)) {
			logger.lifecycle("  SKIPPED {} 已在运行（PID {}）", target.name, pid)
			return
		}
		throw GradleException(
			"端口 ${target.port} 被占用，且不是 devUp 起的进程：${portOwner(target.port)}。" +
				"请先自行释放端口，再重跑 ./gradlew devUp",
		)
	}
	if (pid != null && pidAlive(pid)) {
		throw GradleException(
			"${target.name} 的进程还在（PID $pid）但端口 ${target.port} 没监听，通常是启动失败后 JVM 挂住。" +
				"看日志 ${target.logFile}",
		)
	}
	pid?.let { target.pidFile.delete() }
	logger.lifecycle("  START   {} 端口 {}", target.name, target.port)
	spawn(target)
	awaitReady(target)
}

fun composeCommand(vararg args: String): List<String> =
	listOf("docker", "compose", "-f", devComposeFile.absolutePath) + args

// 项目名由目录名推导，本机另有同名 compose 项目，ps 会列出别人的容器：
// 所以服务清单一律以 config --services 为准，只看本文件声明的服务
fun composeServices(): List<String> {
	val (code, output) = runCapture(composeCommand("config", "--services"))
	if (code != 0) throw GradleException("docker compose config 失败：$output")
	return output.lines().map { it.trim() }.filter { it.isNotEmpty() }
}

fun composeStates(): Map<String, String> {
	val (code, output) = runCapture(composeCommand("ps", "--format", "{{json .}}"))
	if (code != 0) throw GradleException("docker compose ps 失败：$output")
	return output.lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
		val service = Regex(""""Service":"([^"]+)"""").find(line)?.groupValues?.get(1) ?: return@mapNotNull null
		val state = Regex(""""State":"([^"]*)"""").find(line)?.groupValues?.get(1) ?: "none"
		val health = Regex(""""Health":"([^"]*)"""").find(line)?.groupValues?.get(1) ?: ""
		service to if (health.isEmpty()) state else health
	}.toMap()
}

fun middlewareReport(): List<String> {
	val states = composeStates()
	return composeServices().map { "$it=${states[it] ?: "absent"}" }
}

// 就绪判定交给 compose 自己的 healthcheck，这里不再抄一份端口清单
fun awaitMiddleware() {
	val deadline = System.currentTimeMillis() + devMiddlewareTimeout * 1000L
	while (System.currentTimeMillis() < deadline) {
		val report = middlewareReport()
		if (report.isNotEmpty() && report.all { it.endsWith("=healthy") || it.endsWith("=running") }) {
			logger.lifecycle("  READY   中间件 {}", report.joinToString(", "))
			return
		}
		Thread.sleep(1000)
	}
	throw GradleException("中间件在 ${devMiddlewareTimeout}s 内没有全部就绪，当前：${middlewareReport().joinToString(", ")}")
}

subprojects {
	plugins.withId("org.springframework.boot") {
		val javaExtension = extensions.getByType(JavaPluginExtension::class.java)
		// launcher 跟模块自己的 toolchain 同源，根脚本抬 JDK 时这里不用跟着改
		val launcher = extensions.getByType(JavaToolchainService::class.java)
			.launcherFor { languageVersion.set(javaExtension.toolchain.languageVersion) }
		val runtimeClasspath = javaExtension.sourceSets.getByName("main").runtimeClasspath
		val applicationYml = file("src/main/resources/application.yml")
		// 主类走 bootRun（JavaExec 子类，Boot 插件把推断结果填在同一个属性上）：applied script 的 classpath
		// 里没有插件类，硬要引就得再抄一次插件版本，不如用 Gradle 自己的 API
		val mainClassProvider = tasks.named("bootRun").map { (it as JavaExec).mainClass.get() }

		// 跨项目解析配置在 org.gradle.parallel=true 下会被 Gradle 拒（根脚本 versionCoherenceCheck 已踩过一次），
		// 所以每个模块在自己的任务里解析并写出启动输入，根任务只读文件、只做编排。
		// 故意不声明 outputs：class 或依赖变了却被判定 UP-TO-DATE 会留下过期 spec，比每次多跑一次贵。
		tasks.register("devSpec") {
			group = "dev"
			description = "写出本机启动本服务所需的 java 路径、主类与运行时 classpath"
			inputs.files(runtimeClasspath)
			inputs.file(applicationYml)
			// 主类由 resolveMainClassName 产出，必须先跑完再取值。
			// 刻意不用 inputs.property(mainClassProvider) 反推依赖：那条 provider 的 owner 是 bootRun 任务本身，
			// Gradle 会把 bootRun 拉进依赖图，devSpec 于是真的把服务起起来（实测卡住整条构建）。
			dependsOn("resolveMainClassName")
			val specFile = layout.buildDirectory.file("dev/spec.properties")
			val argsFile = layout.buildDirectory.file("dev/java.args")
			doLast {
				val javaExecutable = launcher.get().executablePath.asFile.absolutePath
				val mainClass = mainClassProvider.get()
				val properties = Properties()
				properties.setProperty("java", javaExecutable)
				specFile.get().asFile.apply {
					parentFile.mkdirs()
					OutputStreamWriter(FileOutputStream(this), Charsets.UTF_8).use { out -> properties.store(out, null) }
				}
				// 走 @argfile 而不是 java -cp <classpath>：admin-service 的 classpath 单行实测 29k 字符，
				// 直接拼进命令行会顶到 Windows CreateProcess 的 32767 上限。
				// classpath 不加引号：Java 的 argfile 解析在 Windows 上不会剥掉单引号，加了引号条目就找不到
				// 主类（实测 CNFE）；代价是路径里不能有空格，下面显式拦下来。
				val classpath = runtimeClasspath.asPath
				if (classpath.contains(' ')) {
					throw GradleException(
						"${project.name} 的 classpath 含空格（$classpath），而 java @argfile 需要不加引号才能被正确解析。" +
							"把 Gradle 缓存/JDK 挪到无空格路径，或改回 -cp 直传。",
					)
				}
				argsFile.get().asFile.apply {
					parentFile.mkdirs()
					writeText(
						listOf(
							"-Dfile.encoding=UTF-8",
							"-classpath $classpath",
							mainClass,
						).joinToString("\n") + "\n",
					)
				}
				logger.lifecycle("  devSpec {} 主类 {}", project.name, mainClass)
			}
		}
	}
}

tasks.register("devUp") {
	group = "dev"
	description = "一键起本机开发栈：docker 中间件 + gateway/admin/ecs + 前端，已在运行的跳过"
	dependsOn(devBootModules.map { ":$it:devSpec" })
	doLast {
		devPidDir.get().mkdirs()
		logger.lifecycle("devUp：$devBootModules + $devWebName")
		val (code, output) = runCapture(composeCommand("up", "-d"))
		if (code != 0) throw GradleException("docker compose up -d 失败：\n$output")
		awaitMiddleware()
		devTargets().forEach { startTarget(it) }
		logger.lifecycle("devUp 完成：./gradlew devStatus 看状态，./gradlew devDown 停应用")
	}
}

tasks.register("devDown") {
	group = "dev"
	description = "停掉 devUp 拉起的应用进程（不动 docker 中间件）；没在跑时是 no-op"
	doLast {
		val targets = devTargets().reversed()
		var stopped = 0
		targets.forEach { target ->
			val pid = pidFileValue(target)
			if (pid == null) {
				logger.lifecycle("  IDLE    {}", target.name)
				return@forEach
			}
			if (!pidAlive(pid)) {
				target.pidFile.delete()
				logger.lifecycle("  STALE   {} PID {} 已退出，清掉状态文件", target.name, pid)
				return@forEach
			}
			killTree(pid)
			target.pidFile.delete()
			stopped++
			logger.lifecycle("  STOPPED {} PID {}", target.name, pid)
		}
		if (stopped == 0) logger.lifecycle("devDown：没有在跑的应用进程")
	}
}

tasks.register("devStatus") {
	group = "dev"
	description = "报告 devUp 各目标的进程与端口状态，以及中间件健康"
	doLast {
		devTargets().forEach { target ->
			val pid = pidFileValue(target)
			val state = when {
				pid == null -> "stopped"
				!pidAlive(pid) -> "stale (PID $pid 已退出)"
				!portListening(target.port) -> "no-port (PID $pid, 端口 ${target.port} 未监听)"
				else -> "running (PID $pid, 端口 ${target.port}, ready=${ready(target)})"
			}
			logger.lifecycle("  {}  {}  日志 {}", target.name, state, target.logFile)
		}
		middlewareReport().forEach { logger.lifecycle("  middleware {}", it) }
	}
}

tasks.register("devComposeDown") {
	group = "dev"
	description = "清场：只停本 compose 文件声明的中间件容器（容器与卷都保留）。devDown 不做这件事"
	doLast {
		// 刻意不用 down：项目名由目录名推导，本机存在同名 compose 项目，down 会连带删掉无关容器
		val (code, output) = runCapture(composeCommand("stop") + composeServices())
		if (code != 0) throw GradleException("docker compose stop 失败：\n$output")
		logger.lifecycle(output.trim())
	}
}
