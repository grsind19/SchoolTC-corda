package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object TransferSchema

object TransferSchemaV1: MappedSchema(
        schemaFamily = TransferSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentTransfers::class.java)
){
    @Entity
    @Table(name="transfers")
    class PersistentTransfers(
            @Column(name = "studentId")
            val studentId: String,

            @Column(name = "studentname")
            val studentName : String,

            @Column(name = "dob")
            val dob: String,

            @Column(name = "fatherName")
            val fatherName: String,

            @Column(name = "motherName")
            val motherName: String,


            @Column(name = "gender")
            val gender: String,


            @Column(name = "standard")
            val standard: String,

            @Column(name = "status")
            val status: String,


            @Column(name = "school")
            val school: String,

            @Column(name = "deo")
            val deo: String,

            @Column(name = "originschool")
            val originschool : String,

            @Column(name = "destschool")
            val destschool: String,

            @Column(name = "linear_id")
            var linearId: UUID

    ) : PersistentState(){
        constructor() : this ("","","","", "", "", "", "", "","","", "",  UUID.randomUUID())
    }
}

