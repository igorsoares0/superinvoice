package com.example.superinvoice.data.repository

import com.example.superinvoice.data.Client
import com.example.superinvoice.data.database.dao.ClientDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val clientDao: ClientDao
) {
    fun getActiveClients(): Flow<List<Client>> = clientDao.getActive()

    fun getArchivedClients(): Flow<List<Client>> = clientDao.getArchived()

    suspend fun getClientById(id: Int): Client? = clientDao.getById(id)

    suspend fun insertClient(client: Client): Long = clientDao.insert(client)

    suspend fun updateClient(client: Client) = clientDao.update(client)

    /**
     * Só funciona para cliente sem faturas nem orçamentos: a FK `invoices.clientId` é
     * `RESTRICT` e o banco lança `SQLiteConstraintException`. Cliente com documentos é
     * arquivado (decisão D2), nunca apagado junto com eles.
     */
    suspend fun deleteClient(client: Client) = clientDao.delete(client)

    suspend fun setArchived(client: Client, archived: Boolean) = clientDao.setArchived(client.id, archived)

    suspend fun getClientCount(): Int = clientDao.getCount()
}
