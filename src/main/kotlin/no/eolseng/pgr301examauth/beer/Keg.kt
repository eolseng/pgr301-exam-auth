package no.eolseng.pgr301examauth.beer

import no.eolseng.pgr301examauth.db.User
import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

interface KegRepository : JpaRepository<Keg, Int> {
    fun findAllByOwner(owner: User): Set<Keg>
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
    companion object{
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