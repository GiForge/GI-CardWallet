package com.example.data

import kotlinx.coroutines.flow.Flow

class CardRepository(private val cardDao: CardDao) {
    val allCards: Flow<List<CardEntity>> = cardDao.getAllCards()

    suspend fun insert(card: CardEntity) {
        cardDao.insertCard(card)
    }

    suspend fun deleteById(id: Long) {
        cardDao.deleteCardById(id)
    }
}
