package org.vloyalty.attachments;

//import net.corda.testing.core.DUMMY_BANK_B_NAME
//import net.corda.testing.core.DUMMY_NOTARY_NAME
//import net.corda.testing.node.internal.poll

/*
internal enum class Role {
    SENDER,
    RECIPIENT
}

fun main(args: Array<String>) {
    val parser = OptionParser()

    val roleArg = parser.accepts("role").withRequiredArg().ofType(Role::class.java).required()
    val options = try {
        parser.parse(*args)
    } catch (e: Exception) {
        println(e.message)
        printHelp(parser)
        exitProcess(1)
    }

    val role = options.valueOf(roleArg)!!
    when (role) {
        Role.SENDER -> {
            val host = NetworkHostAndPort("localhost", 10006)
            println("Connecting to sender node ($host)")
            CordaRPCClient(host).start("demo", "demo").use {
                sender(it.proxy)
            }
        }
        Role.RECIPIENT -> {
            val host = NetworkHostAndPort("localhost", 10009)
            println("Connecting to the recipient node ($host)")
            CordaRPCClient(host).start("demo", "demo").use {
                recipient(it.proxy, 10010)
            }
        }
    }
}

/** An in memory test zip attachment of at least numOfClearBytes size, will be used. */
// DOCSTART 2
fun sender(rpc: CordaRPCOps, numOfClearBytes: Int = 1024) { // default size 1K.
    val (inputStream, hash) = InputStreamAndHash.createInMemoryTestZip(numOfClearBytes, 0)
    val executor = Executors.newScheduledThreadPool(2)
    try {
        sender(rpc, inputStream, hash, executor)
    } finally {
        executor.shutdown()
    }
}

private fun sender(rpc: CordaRPCOps, inputStream: InputStream, hash: SecureHash.SHA256, executor: ScheduledExecutorService) {
    // Get the identity key of the other side (the recipient).
    val notaryFuture: CordaFuture<Party> = poll(executor, DUMMY_NOTARY_NAME.toString()) { rpc.wellKnownPartyFromX500Name(DUMMY_NOTARY_NAME) }
    val otherSideFuture: CordaFuture<Party> = poll(executor, DUMMY_BANK_B_NAME.toString()) { rpc.wellKnownPartyFromX500Name(DUMMY_BANK_B_NAME) }
    // Make sure we have the file in storage
    if (!rpc.attachmentExists(hash)) {
        inputStream.use {
            val id = rpc.uploadAttachment(it)
            require(hash == id) { "Id was '$id' instead of '$hash'" }
        }
        require(rpc.attachmentExists(hash)){"Attachment matching hash: $hash does not exist"}
    }

    val flowHandle = rpc.startTrackedFlow(::AttachmentDemoFlow, otherSideFuture.get(), notaryFuture.get(), hash)
    flowHandle.progress.subscribe(::println)
    val stx = flowHandle.returnValue.getOrThrow()
    println("Sent ${stx.id}")
}
// DOCEND 2

@InitiatingFlow
@StartableByRPC
class AttachmentDemoFlow(private val otherSide: Party,
                         private val notary: Party,
                         private val attachId: SecureHash.SHA256) : FlowLogic<SignedTransaction>() {

    object SIGNING : ProgressTracker.Step("Signing transaction")

    override val progressTracker: ProgressTracker = ProgressTracker(SIGNING)

    @Suspendable
    override fun call(): SignedTransaction {
        // Create a trivial transaction with an output that describes the attachment, and the attachment itself
        val ptx = TransactionBuilder(notary)
                .addOutputState(AttachmentContract.State(attachId), ATTACHMENT_PROGRAM_ID)
                .addCommand(AttachmentContract.Command, ourIdentity.owningKey)
                .addAttachment(attachId)

        progressTracker.currentStep = SIGNING

        val stx = serviceHub.signInitialTransaction(ptx)

        // Send the transaction to the other recipient
        return subFlow(FinalityFlow(stx, initiateFlow(otherSide)))
    }
}

@InitiatedBy(AttachmentDemoFlow::class)
class StoreAttachmentFlow(private val otherSide: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // As a non-participant to the transaction we need to record all states
        subFlow(ReceiveFinalityFlow(otherSide, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}

@StartableByRPC
@StartableByService
class NoProgressTrackerShellDemo : FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        return "You Called me!"
    }
}


@Suppress("DEPRECATION")
// DOCSTART 1
fun recipient(rpc: CordaRPCOps, webPort: Int) {
    println("Waiting to receive transaction ...")
    val stx = rpc.internalVerifiedTransactionsFeed().updates.toBlocking().first()
    val wtx = stx.tx
    if (wtx.attachments.isNotEmpty()) {
        if (wtx.outputs.isNotEmpty()) {
            val state = wtx.outputsOfType<AttachmentContract.State>().single()
            require(rpc.attachmentExists(state.hash)) {"attachment matching hash: ${state.hash} does not exist"}

            // Download the attachment via the Web endpoint.
            val connection = URL("http://localhost:$webPort/attachments/${state.hash}").openConnection() as HttpURLConnection
            try {
                require(connection.responseCode == SC_OK) { "HTTP status code was ${connection.responseCode}" }
                require(connection.contentType == APPLICATION_OCTET_STREAM) { "Content-Type header was ${connection.contentType}" }
                require(connection.getHeaderField(CONTENT_DISPOSITION) == "attachment; filename=\"${state.hash}.zip\"") {
                    "Content-Disposition header was ${connection.getHeaderField(CONTENT_DISPOSITION)}"
                }

                // Write out the entries inside this jar.
                println("Attachment JAR contains these entries:")
                JarInputStream(connection.inputStream).use { it ->
                    while (true) {
                        val e = it.nextJarEntry ?: break
                        println("Entry> ${e.name}")
                        it.closeEntry()
                    }
                }
            } finally {
                connection.disconnect()
            }
            println("File received - we're happy!\n\nFinal transaction is:\n\n${Emoji.renderIfSupported(wtx)}")
        } else {
            println("Error: no output state found in ${wtx.id}")
        }
    } else {
        println("Error: no attachments found in ${wtx.id}")
    }
}
// DOCEND 1

private fun printHelp(parser: OptionParser) {
    println("""
    Usage: attachment-demo --role [RECIPIENT|SENDER] [options]
    Please refer to the documentation in docs/build/index.html for more info.

    """.trimIndent())
    parser.printHelpOn(System.out)
}

const val ATTACHMENT_PROGRAM_ID = "org.vloyalty.attachments.AttachmentContract"

class AttachmentContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val state = tx.outputsOfType<State>().single()
        // we check that at least one has the matching hash, the other will be the contract
        require(tx.attachments.any { it.id == state.hash }) {"At least one attachment in transaction must match hash ${state.hash}"}
    }

    object Command : TypeOnlyCommandData()

    data class State(val hash: SecureHash.SHA256) : ContractState {
        override val participants: List<AbstractParty> = emptyList()
    }
}
*/