package com.ramitsuri.notificationjournal.core.data.dictionary

interface DictionaryDao {
    suspend fun getItems(): List<DictionaryItem>
}

class DictionaryDaoImpl : DictionaryDao {
    override suspend fun getItems(): List<DictionaryItem> {
        return listOf()
    }
}
