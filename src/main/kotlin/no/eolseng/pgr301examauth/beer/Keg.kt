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
        const val MIN_CAPACITY: Int = 100
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