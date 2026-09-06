package example.exceltoanki.ui

import example.exceltoanki.CardBuildProgressSubscriber

/**
 * UI implementation of CardBuildProgressSubscriber that updates the conversion progress.
 * Tracks card creation progress and invokes callbacks to update the UI.
 */
class UIProgressSubscriber(
    val totalItems: Int,
    val onProgress: (Float, String) -> Unit
) : CardBuildProgressSubscriber {

    private var createdCount = 0

    override fun onCardCreated(currentCount: Int, totalCount: Int) {
        createdCount = currentCount
        val progress = (currentCount.toFloat() / totalCount.toFloat())
        val message = "Building cards: $currentCount/$totalCount"
        onProgress(progress, message)
    }

    override fun onCardError(cardIndex: Int, error: String) {
        System.err.println("Error on card ${cardIndex + 1}: $error")
    }

    fun getProgress(): Float {
        return if (totalItems > 0) (createdCount.toFloat() / totalItems.toFloat()) else 0f
    }
}
