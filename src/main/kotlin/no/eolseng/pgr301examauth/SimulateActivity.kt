package no.eolseng.pgr301examauth

import no.eolseng.pgr301examauth.beer.*
import no.eolseng.pgr301examauth.db.UserRepository
import no.eolseng.pgr301examauth.db.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private const val TEST_USERNAME = "test_user"
private const val KEG_AMOUNT = 30

/**
 * Used to insert test data to help showcase the application metrics
 */
@Component
class TestDataInsertion(
        private val userService: UserService,
        private val kegService: KegService,
        private val userRepo: UserRepository,
        private val mugRepo: MugRepository
) : CommandLineRunner {
    override fun run(vararg args: String) {
        // Create a test user
        userService.createUser(TEST_USERNAME, "Test_Password", setOf("USER"))
        val user = userRepo.findById(TEST_USERNAME).get()
        // Create kegs
        for (x in 0 until KEG_AMOUNT) {
            // Get random keg capacity
            val capacity = (Keg.MIN_CAPACITY..Keg.MAX_CAPACITY).random()
            // Persist the keg
            kegService.createKeg(user, capacity)
        }
        // Create a mug
        val mug = Mug(owner = user, capacity = Mug.Capacity.HUGE)
        mugRepo.save(mug)
    }
}

@Component
class TestDataActivity(
        private val userRepo: UserRepository,
        private val kegRepo: KegRepository,
        private val mugRepo: MugRepository,
        private val tapService: TapService,
        private val sipService: SipService
) {
    /**
     * Simulates activity in the application to get metric data
     * It taps a mug of beer and takes a random amount of sips from the test users kegs
     */
    @Scheduled(fixedDelay = 3 * 1000)
    fun randomTapAndSip() {
        if (userRepo.existsById(TEST_USERNAME)){
            val user = userRepo.findById(TEST_USERNAME).get()
            val kegs = kegRepo.findAllByOwner(user)
            val mug = mugRepo.findAllByOwner(user).first()

            kegs.forEach { keg ->
                // Fill the mug with beer from the keg
                tapService.tapBeer(keg.id, mug.id, user.username)
                // Calculate amount of sips to take - might be more than volume of mug
                val sips = (0..mug.currentVolume + 1).random()
                // Take sips amount of sips
                for (x in 0..sips) sipService.takeSip(mug.id, user.username)
            }
        }
    }
}
