package com.example.api

import com.example.flow.StudentCreateFlow.Initiator
import com.example.flow.StudentUpdateFlow.InitiatorUU
import com.example.flow.TransferApproveFlow.InitiatorTA
import com.example.flow.TransferCreateFlow.InitiatorTC
import com.example.model.Student
import com.example.model.Transfer
import com.example.schema.StudentSchemaV1
import com.example.state.StudentState
import com.example.state.TransferState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }


    @GET
    @Path("students")
    @Produces(MediaType.APPLICATION_JSON)
    fun mystudent(): Response {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = builder {
            val criteria = generalCriteria
            val results = rpcOps.vaultQueryBy<StudentState>(criteria).states
            return Response.ok(results).build()
        }
    }

    @GET
    @Path("transfers")
    @Produces(MediaType.APPLICATION_JSON)
    fun transfers(): Response {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = builder {
            val criteria = generalCriteria
            val results = rpcOps.vaultQueryBy<TransferState>(criteria).states
            return Response.ok(results).build()
        }
    }

    @GET
    @Path("student")
    @Produces(MediaType.APPLICATION_JSON)
    fun student(@QueryParam("studentId") studentId: String): Response {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = builder {
            var partyType = StudentSchemaV1.PersistentStudents::studentId.equal(studentId.toString())
            val customCriteria = QueryCriteria.VaultCustomQueryCriteria(partyType)
            val criteria = generalCriteria.and(customCriteria)
            val results = rpcOps.vaultQueryBy<StudentState>(criteria).states
            return Response.ok(results).build()
        }
    }

    @GET
    @Path("create-student")
    @Produces(MediaType.APPLICATION_JSON)
    fun createStudent(@QueryParam("studentId") studentId: String,
                      @QueryParam("studentName") studentName: String,
                      @QueryParam("dob") dob: String,
                      @QueryParam("fatherName") fatherName: String,
                      @QueryParam("motherName") motherName: String,
                      @QueryParam("gender") gender: String,
                      @QueryParam("standard") standard: String,
                      @QueryParam("school") school: CordaX500Name,
                      @QueryParam("deo") deo: CordaX500Name): Response {
        val schoolparty = rpcOps.wellKnownPartyFromX500Name(school)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()

        val deoparty = rpcOps.wellKnownPartyFromX500Name(deo)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()
        val student = Student(studentId, studentName, fatherName, motherName, dob, standard, gender, schoolparty, deoparty)
        return try {
            val signedTx = rpcOps.startTrackedFlow(::Initiator, student).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @GET
    @Path("update-student")
    @Produces(MediaType.APPLICATION_JSON)
    fun updateStudent(@QueryParam("studentId") studentId: String,
                      @QueryParam("studentName") studentName: String,
                      @QueryParam("dob") dob: String,
                      @QueryParam("fatherName") fatherName: String,
                      @QueryParam("motherName") motherName: String,
                      @QueryParam("gender") gender: String,
                      @QueryParam("standard") standard: String,
                      @QueryParam("school") school: CordaX500Name,
                      @QueryParam("deo") deo: CordaX500Name): Response {
        val schoolparty = rpcOps.wellKnownPartyFromX500Name(school)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()

        val deoparty = rpcOps.wellKnownPartyFromX500Name(deo)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()
        val student = Student(studentId, studentName, fatherName, motherName, dob, standard, gender, schoolparty, deoparty)
        return try {
            val signedTx = rpcOps.startTrackedFlow(::InitiatorUU, student).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @GET
    @Path("create-transfer")
    @Produces(MediaType.APPLICATION_JSON)
    fun createTransfer(@QueryParam("studentId") studentId: String,
                       @QueryParam("studentName") studentName: String,
                       @QueryParam("dob") dob: String,
                       @QueryParam("fatherName") fatherName: String,
                       @QueryParam("motherName") motherName: String,
                       @QueryParam("gender") gender: String,
                       @QueryParam("standard") standard: String,
                       @QueryParam("school") school: CordaX500Name,
                       @QueryParam("deo") deo: CordaX500Name,
                       @QueryParam("origin") origin: CordaX500Name,
                       @QueryParam("dest") dest: CordaX500Name): Response {

        val schoolparty = rpcOps.wellKnownPartyFromX500Name(school)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()

        val deoparty = rpcOps.wellKnownPartyFromX500Name(deo)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()


        val oschoolparty = rpcOps.wellKnownPartyFromX500Name(origin)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()

        val dschoolparty = rpcOps.wellKnownPartyFromX500Name(dest)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()


        val transfer = Transfer(oschoolparty, dschoolparty, studentId, studentName, fatherName, motherName, dob, standard, gender, schoolparty, deoparty)
        return try {
            val signedTx = rpcOps.startTrackedFlow(::InitiatorTC, transfer).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @GET
    @Path("approve-transfer")
    @Produces(MediaType.APPLICATION_JSON)
    fun approveTransfer(@QueryParam("studentId") studentId: String,
                        @QueryParam("studentName") studentName: String,
                        @QueryParam("dob") dob: String,
                        @QueryParam("fatherName") fatherName: String,
                        @QueryParam("motherName") motherName: String,
                        @QueryParam("gender") gender: String,
                        @QueryParam("standard") standard: String,
                        @QueryParam("school") school: CordaX500Name,
                        @QueryParam("deo") deo: CordaX500Name,
                        @QueryParam("origin") origin: CordaX500Name,
                        @QueryParam("dest") dest: CordaX500Name): Response {
        val schoolparty = rpcOps.wellKnownPartyFromX500Name(school)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()

        val deoparty = rpcOps.wellKnownPartyFromX500Name(deo)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()


        val oschoolparty = rpcOps.wellKnownPartyFromX500Name(origin)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()

        val dschoolparty = rpcOps.wellKnownPartyFromX500Name(dest)
                ?: return Response.status(BAD_REQUEST).entity("Party named $school cannot be found.\n").build()


        val transfer = Transfer(oschoolparty, dschoolparty, studentId, studentName, fatherName, motherName, dob, standard, gender, schoolparty, deoparty)
        return try {
            val signedTx = rpcOps.startTrackedFlow(::InitiatorTA, transfer).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }
}