package no.eolseng.pgr301examauth.beer

import io.micrometer.core.annotation.Timed
import no.eolseng.pgr301examauth.db.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Service
import javax.persistence.*
import javax.transaction.Transactional
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

interface KegRepository : JpaRepository<Keg, Int> {

    fun findAllByOwner(owner: User): Set<Keg>

    @Query("SELECT SUM(k.capacity) FROM Keg k")
    fun getTotalCapacity(): Int

    @Query("SELECT SUM(k.currentVolume) FROM Keg k")
    fun getTotalVolume(): Int

    @Query("SELECT k.id FROM Keg k")
    fun getAllIds(): List<Int>

}

@Service
class KegService(
        private val kegRepo: KegRepository
) {

    @Timed(description = "Time of filling a single keg", value = "beer.kegs.fill.single")
    fun fillKeg(keg: Keg) {
        // Calculate amount to fill
        val amountToFill = keg.capacity - keg.currentVolume
        // Sleep for the amount to fill in millis
        Thread.sleep(amountToFill.toLong() * 10)
        // Fill
        keg.currentVolume += amountToFill
        // Persist
        kegRepo.save(keg)
    }

    @Transactional
    fun getAvgKeg(): Double {
        return kegRepo.getTotalVolume().toDouble() / kegRepo.getTotalCapacity()
    }

}

@Entity
@Table(name = "KEGS")
class Keg(

        @get:Id
        @get:GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Int = 0,

        @get:ManyToOne
        @get:NotNull
        var owner: User? = null,

        @get:Min(MIN_CAPACITY.toLong())
        @get:Max(MAX_CAPACITY.toLong())
        var capacity: Int = 10,

        @get:Min(0)
        @get:Max(MAX_CAPACITY.toLong())
        var currentVolume: Int = 0

) {
    companion object {
        const val MIN_CAPACITY: Int = 0
        const val MAX_CAPACITY: Int = 600
    }
}

data class KegDto(
        val id: Int? = null,
        val ownerName: String? = null,
        @get:Min(Keg.MIN_CAPACITY.toLong(), message = "Keg size must be between ${Keg.MIN_CAPACITY} dl and ${Keg.MAX_CAPACITY} dl.")
        @get:Max(Keg.MAX_CAPACITY.toLong(), message = "Keg size must be between ${Keg.MIN_CAPACITY} dl and ${Keg.MAX_CAPACITY} dl.")
        val capacity: Int? = null,

        @get:Min(0, message = "Volume cannot be negative")
        @get:Max(Keg.MAX_CAPACITY.toLong(), message = "Volume cannot be bigger than max keg size (${Keg.MAX_CAPACITY} dl)")
        val currentVolume: Int = 0
)

fun Keg.toDto(): KegDto {
    return KegDto(
            id = id,
            ownerName = owner?.username,
            capacity = capacity,
            currentVolume = currentVolume
    )
}