package no.eolseng.pgr301examauth.beer

import io.micrometer.core.annotation.Timed
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BreweryScheduling(
        private val kegRepo: KegRepository,
        private val kegService: KegService
) {

    @Scheduled(fixedDelay = 60 * 1000)
    @Timed(description = "Time of filling all kegs", value = "beer.kegs.fill.all", longTask = true)
    fun refillAllKegs() {
        // Add 15 second load up time
        Thread.sleep(15 * 1000)
        kegRepo.getAllIds()
                .forEach {
                    val keg = kegRepo.findById(it).get()
                    kegService.fillKeg(keg)
                }
    }

}