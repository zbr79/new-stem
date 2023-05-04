package com.example.final_project.models

import com.example.final_project.utils.DEFAULT_ICONS


// Constructing number of cards based on board size
// then picking random images and create memoryCard with it
class MemoryGame(private val boardSize: BoardSize){

    val cards: List<MemoryCard>
    var numPairsFound = 0

    private var numCardFlips = 0
    private var indexOfSingleSelectedCard: Int? = null

    init{
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages).shuffled()

        //Transform randomize images into new list
        cards = randomizedImages.map{ MemoryCard(it) }
    }

    //Handle checking if 2 card flipped is equal or not
    fun flipCard(position: Int):Boolean{
        numCardFlips++
        val card: MemoryCard = cards[position]
        /*
         3 cases:
             0 card previously flipped over => flip over selected card
             1 card  previously flipped over => flip over selected card + check if images match
             2 cards previously flipped over => restored cards + flip over selected card
         */
        var foundMatch = false
        if (indexOfSingleSelectedCard == null){
            // 0 or 2 card flipped
            restoreCards()
            indexOfSingleSelectedCard = position
        }
        else{
            // 1 card flipped
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if(cards[position1].identifier != cards[position2].identifier){
            //user pick incorrectly
            return false
        }
        //if they match
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        // iterate over list of all cards
        // if card is not match, face down
            for (card: MemoryCard in cards) {
                if (!card.isMatched) {
                    card.isFaceUp = false
                }
            }
    }

    fun haveWonGame(): Boolean {
        // found number of pairs == total pairs
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        //Grab the card at that position and check if it face up
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numCardFlips / 2
    }
}