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
    fun getAllClients(): Flow<List<Client>> = clientDao.getAll()

    suspend fun getClientById(id: Int): Client? = clientDao.getById(id)

    suspend fun insertClient(client: Client): Long = clientDao.insert(client)

    suspend fun updateClient(client: Client) = clientDao.update(client)

    suspend fun deleteClient(client: Client) = clientDao.delete(client)

    suspend fun getClientCount(): Int = clientDao.getCount()
}
