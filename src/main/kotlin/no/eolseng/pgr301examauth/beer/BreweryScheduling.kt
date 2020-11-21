package no.eolseng.pgr301examauth.beer

import io.micrometer.core.annotation.Timed
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BreweryScheduling(
        private val kegRepo: KegRepository,
        private val kegService: KegService
) {
    /**
     * Fills all non-empty kegs every 60 seconds
     * Has a 15 second warm up time. Filling a keg takes time as well - see 'KegService.fillKeg()'.
     */
    @Scheduled(fixedDelay = 60 * 1000)
    @Timed(description = "Time of filling all kegs", value = "beer.kegs.fill.all", longTask = true)
    fun refillAllKegs() {
        // Add 15 second load up time
        Thread.sleep(15 * 1000)
        // Get all kegs, filter only non-full and fill them
        kegRepo.findAll()
                .filter { it.currentVolume < it.capacity }
                .forEach { kegService.fillKeg(it) }
    }

}