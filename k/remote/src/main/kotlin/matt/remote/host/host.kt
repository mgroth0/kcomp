package matt.remote.host

//import matt.kjlib.commons.USER_HOME
import com.jcraft.jsch.JSch
import com.jcraft.jsch.UserInfo
import matt.file.commons.USER_HOME
import matt.remote.expect.expect
import matt.remote.host.Host.Companion.SUB_PROMPT
import matt.remote.om.OM
import net.sf.expectit.Expect
import net.sf.expectit.ExpectBuilder
import net.sf.expectit.ExpectIOException

class Host(private val hostname: String) {

  companion object {
	/*used to match the command-line prompt*/
	const val SUB_PROMPT = "PEXPECT"
	private const val UNIQUE_PROMPT = "\\[$SUB_PROMPT\\][\\$\\#] "
	const val PROMPT = UNIQUE_PROMPT

	/*used to set shell command-line prompt to UNIQUE_PROMPT.*/
	const val PROMPT_SET_SH = "PS1='[$SUB_PROMPT]\\$ '"

	val PASS_LONG by lazy { USER_HOME[".passlong"].readText().reversed() }
  }


  fun ssh(vararg echos: java.lang.Appendable, op: Expect.()->Unit) {

	val jSch = JSch().apply {
	  setKnownHosts(USER_HOME[".ssh"]["known_hosts"].absolutePath)

	  /*addIdentity(USER_HOME[".ssh"]["id_rsa"].absolutePath*//*, HOME_DIR[".passlong"].readText()*//*)*/

	}
	val session = jSch.getSession(OM.USER, hostname).apply {
	  setPassword(PASS_LONG)
	}

	session.userInfo = object: UserInfo {
	  override fun getPassphrase() = PASS_LONG
	  override fun getPassword() = PASS_LONG
	  override fun promptPassword(message: String) = true
	  override fun promptPassphrase(message: String) = true
	  override fun promptYesNo(message: String) = true
	  override fun showMessage(message: String) {}
	}

	session.connect()
	val channel = session.openChannel("shell")
	channel.connect()

	val p = ExpectBuilder()
	  .withInputs(channel.inputStream, channel.extInputStream)
	  .withOutput(channel.outputStream)
	  .withEchoInput(object: Appendable {
		override fun append(csq: CharSequence?): java.lang.Appendable {
		  System.out.append(csq)
		  echos.forEach { it.append(csq) }
		  return this
		}

		override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable {
		  System.out.append(csq, start, end)
		  echos.forEach { it.append(csq, start, end) }
		  return this
		}

		override fun append(c: Char): java.lang.Appendable {
		  System.out.append(c)
		  echos.forEach { it.append(c) }
		  return this
		}

	  })
	  .withEchoOutput(System.err)
	  .withExceptionOnFailure()
	  .withInfiniteTimeout()
	  .build()

	println("established SSH connection to $hostname")
	try {
	  p expect "Last login"
	  p.setPrompt()


	  op(p)
	} catch (e: ExpectIOException) {
	  println(e)
	  println(e.message)
	  e.printStackTrace()
	  println("caught $e, now closing and disconnecting ssh session")
	}
	p.close()
	channel.disconnect()
	session.disconnect()
	println("disconnected")
  }

}

fun Expect.prompt() = expect(SUB_PROMPT)
fun Expect.setPrompt(/*numExpectPrompts: Int = 3*/) {
  println("setPrompt1")
  sendLine(Host.PROMPT_SET_SH)
  /*if (numExpectPrompts > 1) {
	(2..numExpectPrompts).forEach {
	  println("setPrompt${it}")
	  prompt()
	}
  }*/
  catchUp()
  println("setPromptEnd")
}

fun Expect.catchUp() {
  /*use any environmental var that's consistent in all shells*/
  /*this is tested in base OM, srun, and singularity. Not yet tested in vagrant.*/
  sendLine("echo \$NCARG_FONTCAPS")
  expect("/usr/lib64/ncarg/fontcaps")
  prompt()
}

object Hosts {
  val OM = Host("openmind.mit.edu")

  @Suppress("unused")
  val POLESTAR = Host("polestar.mit.edu")
}
