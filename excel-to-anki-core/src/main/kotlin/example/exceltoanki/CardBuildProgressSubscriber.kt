package example.exceltoanki

/**
 * Subscriber interface for tracking progress during card building.
 * Implementations can update UI, log progress, or perform other actions.
 */
interface CardBuildProgressSubscriber {
    /**
     * Called when a card is successfully created.
     * @param currentCount The current number of cards created
     * @param totalCount The total number of cards to create
     */
    fun onCardCreated(currentCount: Int, totalCount: Int)

    /**
     * Called when an error occurs during card creation.
     * @param cardIndex The index of the card that failed (0-based)
     * @param error The error message
     */
    fun onCardError(cardIndex: Int, error: String)
}
