package matt.remote.expect

import matt.remote.host.prompt
import net.sf.expectit.Expect
import net.sf.expectit.Result
import net.sf.expectit.matcher.Matchers
import java.io.File

infix fun Expect.sendLineAndWait(s: String): Expect = sendLine(s).apply { prompt() }
infix fun Expect.expect(s: String): Result = expect(Matchers.contains(s))

val File.ensureAbsolute get() = apply { require(isAbsolute) }
val File.absolutePathEnforced: String get() = ensureAbsolute.absolutePath
infix fun Expect.cd(dir: String) = sendLineAndWait("cd \"$dir\"")
infix fun Expect.cd(file: File) = cd(file.absolutePathEnforced)
fun Expect.pwd() = sendLineAndWait("pwd")
fun Expect.ls() = sendLineAndWait("ls")
fun Expect.exit() = sendLineAndWait("exit")
fun Expect.mkdir(name: String) = sendLineAndWait("mkdir \"$name\"")
fun Expect.mkdir(file: File) = apply { mkdir(file.absolutePathEnforced) }
fun Expect.writeFile(filename: String, s: String) =
	sendLineAndWait("echo \"${s.replace("\"", "\\\"")}\" > \"$filename\"")

fun Expect.writeFile(file: File, s: String) =
	writeFile(filename = file.absolutePathEnforced, s = s)

fun Expect.rm(filename: String, rf: Boolean = false) {
  if (rf) sendLineAndWait("rm -rf \"${filename}\"")
  else sendLineAndWait("rm \"${filename}\"")
}

fun Expect.rm(file: File, rf: Boolean = false) = rm(file.absolutePathEnforced, rf = rf)

fun Expect.echo(s: String) = sendLineAndWait("echo \"$s\"")
