package com.example.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Student(val studentId: String,
                   val studentName: String,
                   val fatherName: String,
                   val motherName: String,
                   val dob: String,
                   val standard: String,
                   val gender: String,
                   val school: Party,
                   val deo: Party)