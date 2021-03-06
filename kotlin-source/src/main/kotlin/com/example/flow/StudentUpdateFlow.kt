package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.StudentContract
import com.example.model.Student
import com.example.schema.StudentSchemaV1
import com.example.state.StudentState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object StudentUpdateFlow {
    @InitiatingFlow
    @StartableByRPC
    class InitiatorUU (val student: Student) : FlowLogic<SignedTransaction>(){
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Student.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override  fun call(): SignedTransaction{
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
            val partyType = StudentSchemaV1.PersistentStudents::studentId.equal(student.studentId.toString())
            val customCriteria = QueryCriteria.VaultCustomQueryCriteria(partyType)
            val criteria = generalCriteria.and(customCriteria)
            val results = serviceHub.vaultService.queryBy<StudentState>(criteria)

            val studentState = StudentState(student.studentId, student.studentName, student.dob, student.fatherName, student.motherName, student.gender, student.standard, "N", student.school, student.deo)
            val txCommand = Command(StudentContract.Commands.Update(), studentState.participants.map { it.owningKey })
            System.out.println(results.states.size)
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(results.states[0])
                    .addOutputState(studentState, StudentContract.STUDENT_CONTRACT_ID)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartyFlow = initiateFlow(student.deo)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))

        }
    }


    @InitiatedBy(InitiatorUU::class)
    class AcceptorUU(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an student transaction." using (output is StudentState)
                    val out = output as StudentState
                    "Student Id should not empty" using (out.studentId!="")
                }
            }

            return subFlow(signTransactionFlow)
        }
    }

}