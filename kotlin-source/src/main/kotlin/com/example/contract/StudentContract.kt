package com.example.contract

import com.example.state.StudentState
import com.example.state.TransferState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class StudentContract : Contract {

    companion object {
        @JvmStatic
        val STUDENT_CONTRACT_ID = "com.example.contract.StudentContract"

    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.get(0)
        when (command.value) {
            is StudentContract.Commands.Create -> requireThat {
                val out = tx.outputsOfType<StudentState>().single()
                "studentName should not be empty" using (out.studentName != "")
                "fathername should not be empty" using (out.fatherName != "")
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
            }
            is StudentContract.Commands.Update -> requireThat {
                val out = tx.outputsOfType<StudentState>().single()
                "studentName should not be empty" using (out.studentName != "")
                "fathername should not be empty" using (out.fatherName != "")
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
            }

            else -> throw IllegalArgumentException("Unrecognised command")
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
    }
}