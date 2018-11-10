package com.example.state

import com.example.schema.TransferSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class TransferState(
        val originSchool: Party,
        val destSchool: Party,
        val studentId: String,
        val studentName: String,
        val dob: String,
        val fatherName: String,
        val motherName: String,
        val gender: String,
        val standard : String,
        val status: String,
        val school:Party,
        val deo: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
): QueryableState, LinearState, ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(originSchool, destSchool, deo)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is TransferSchemaV1 -> TransferSchemaV1.PersistentTransfers(
                    this.studentId,
                    this.studentName,
                    this.dob,
                    this.fatherName,
                    this.motherName,
                    this.gender,
                    this.standard,
                    this.status,
                    this.school.toString(),
                    this.deo.toString(),
                    this.originSchool.toString(),
                    this.destSchool.toString(),
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas(): Iterable<MappedSchema> {
        return  listOf(TransferSchemaV1)
    }
}