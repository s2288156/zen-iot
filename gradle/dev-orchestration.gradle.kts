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
	throw GradleException("$yml has no server.port")
}

fun readVitePort(): Int {
	val ts = File(rootDir, "$devWebName/vite.config.ts")
	return Regex("""\bport:\s*(\d+)""").find(ts.readText())?.groupValues?.get(1)?.toInt()
		?: throw GradleException("$ts has no port in its server block: dev needs a fixed port, otherwise probing drifts when Vite falls back to another port")
}

fun runCapture(args: List<String>): Pair<Int, String> {
	val process = try {
		ProcessBuilder(args).redirectErrorStream(true).start()
	} catch (e: Exception) {
		throw GradleException("command failed: ${args.joinToString(" ")} (${e.message})", e)
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
			if (!file.isFile) throw GradleException("$file is missing; run ./gradlew :$module:devSpec first")
			file.inputStream().use { load(it.reader(Charsets.UTF_8)) }
		}
		val args = File(rootDir, "$module/build/dev/java.args")
		if (!args.isFile) throw GradleException("$args is missing; run ./gradlew :$module:devSpec first")
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
		if (!entry.isFile) throw GradleException("cannot find $entry; run pnpm install in web/ first")
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
	} ?: return "unknown"
	return "PID $pid (${ProcessHandle.of(pid).flatMap { h -> h.info().command() }.orElse("command unknown")})"
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
			logger.lifecycle("  READY   {} port {}", target.name, target.port)
			return
		}
		if (System.currentTimeMillis() - tick > 5000) {
			logger.lifecycle("  waiting {} ...", target.name)
			tick = System.currentTimeMillis()
		}
		Thread.sleep(500)
	}
	val tail = target.logFile.takeIf { it.isFile }?.readLines()?.takeLast(20)?.joinToString("\n") ?: "(no log yet)"
	throw GradleException(
		"${target.name} did not become ready within ${target.waitSeconds}s (port ${target.port}). Log tail:\n$tail\n" +
			"Full log: ${target.logFile}",
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
		throw GradleException("failed to start ${target.name}: ${e.message}", e)
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
			logger.lifecycle("  SKIPPED {} already running (PID {})", target.name, pid)
			return
		}
		throw GradleException(
			"port ${target.port} is occupied by a process devUp did not start: ${portOwner(target.port)}. " +
				"Free the port yourself, then re-run ./gradlew devUp",
		)
	}
	if (pid != null && pidAlive(pid)) {
		throw GradleException(
			"${target.name} still has a live process (PID $pid) but port ${target.port} is not listening - usually a JVM left hanging after a failed startup. " +
				"Check the log ${target.logFile}",
		)
	}
	pid?.let { target.pidFile.delete() }
	logger.lifecycle("  START   {} port {}", target.name, target.port)
	spawn(target)
	awaitReady(target)
}

fun composeCommand(vararg args: String): List<String> =
	listOf("docker", "compose", "-f", devComposeFile.absolutePath) + args

// 项目名由目录名推导，本机另有同名 compose 项目，ps 会列出别人的容器：
// 所以服务清单一律以 config --services 为准，只看本文件声明的服务
fun composeServices(): List<String> {
	val (code, output) = runCapture(composeCommand("config", "--services"))
	if (code != 0) throw GradleException("docker compose config failed: $output")
	return output.lines().map { it.trim() }.filter { it.isNotEmpty() }
}

fun composeStates(): Map<String, String> {
	val (code, output) = runCapture(composeCommand("ps", "--format", "{{json .}}"))
	if (code != 0) throw GradleException("docker compose ps failed: $output")
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
			logger.lifecycle("  READY   middleware {}", report.joinToString(", "))
			return
		}
		Thread.sleep(1000)
	}
	throw GradleException("middleware did not all become ready within ${devMiddlewareTimeout}s, current: ${middlewareReport().joinToString(", ")}")
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
			description = "Write the java path, main class and runtime classpath needed to start this service locally"
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
						"${project.name} has spaces in its classpath ($classpath), and java @argfile parses entries correctly only when unquoted. " +
							"Move the Gradle cache/JDK to a path without spaces, or go back to passing -cp directly.",
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
				logger.lifecycle("  devSpec {} main class {}", project.name, mainClass)
			}
		}
	}
}

tasks.register("devUp") {
	group = "dev"
	description = "Start the local dev stack in one go: docker middleware + gateway/admin/ecs + web, skipping what already runs"
	dependsOn(devBootModules.map { ":$it:devSpec" })
	doLast {
		devPidDir.get().mkdirs()
		logger.lifecycle("devUp: $devBootModules + $devWebName")
		val (code, output) = runCapture(composeCommand("up", "-d"))
		if (code != 0) throw GradleException("docker compose up -d failed:\n$output")
		awaitMiddleware()
		devTargets().forEach { startTarget(it) }
		logger.lifecycle("devUp done: ./gradlew devStatus to check state, ./gradlew devDown to stop the apps")
	}
}

tasks.register("devDown") {
	group = "dev"
	description = "Stop the app processes started by devUp (docker middleware untouched); no-op when nothing is running"
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
				logger.lifecycle("  STALE   {} PID {} exited, clearing state file", target.name, pid)
				return@forEach
			}
			killTree(pid)
			target.pidFile.delete()
			stopped++
			logger.lifecycle("  STOPPED {} PID {}", target.name, pid)
		}
		if (stopped == 0) logger.lifecycle("devDown: no app processes running")
	}
}

tasks.register("devStatus") {
	group = "dev"
	description = "Report process and port state for each devUp target, plus middleware health"
	doLast {
		devTargets().forEach { target ->
			val pid = pidFileValue(target)
			val state = when {
				pid == null -> "stopped"
				!pidAlive(pid) -> "stale (PID $pid exited)"
				!portListening(target.port) -> "no-port (PID $pid, port ${target.port} not listening)"
				else -> "running (PID $pid, port ${target.port}, ready=${ready(target)})"
			}
			logger.lifecycle("  {}  {}  log {}", target.name, state, target.logFile)
		}
		middlewareReport().forEach { logger.lifecycle("  middleware {}", it) }
	}
}

tasks.register("devComposeDown") {
	group = "dev"
	description = "Teardown: stop only the middleware containers this compose file declares (containers and volumes kept); devDown never does this"
	doLast {
		// 刻意不用 down：项目名由目录名推导，本机存在同名 compose 项目，down 会连带删掉无关容器
		val (code, output) = runCapture(composeCommand("stop") + composeServices())
		if (code != 0) throw GradleException("docker compose stop failed:\n$output")
		logger.lifecycle(output.trim())
	}
}
