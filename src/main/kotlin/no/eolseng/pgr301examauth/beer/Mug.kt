package no.eolseng.pgr301examauth.beer

import no.eolseng.pgr301examauth.db.User
import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

interface MugRepository : JpaRepository<Mug, Int> {
    fun findAllByOwner(owner: User): Set<Mug>
}

@Entity
class Mug(

        @get:Id
        @get:GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Int? = null,

        @get:NotNull
        @get:ManyToOne
        var owner: User? = null,

        @get:NotNull
        var capacity: Capacity? = null,

        @get:Min(0)
        @get:Max(6)
        var currentVolume: Int = 0

) {
    enum class Capacity(val volume: Int) {
        SMALL(3),
        MEDIUM(4),
        LARGE(5),
        HUGE(6)
    }

    companion object {
        private val VALUES = Capacity.values()
        fun getCapacityByInt(volume: Int) = VALUES.firstOrNull { it.volume == volume }
    }
}

data class MugDto(
        val id: Int? = null,
        val ownerName: String? = null,
        @get:Min(3, message = "Mug size must be between 3 dl and 6 dl.")
        @get:Max(6, message = "Mug size must be between 3 dl and 6 dl.")
        val capacity: Int? = null,

        @get:Min(0, message = "Volume cannot be negative")
        @get:Max(6, message = "Volume cannot be bigger than max mug size (6 dl)")
        val currentVolume: Int = 0
)

fun Mug.toDto(): MugDto {
    return MugDto(
            id = id,
            ownerName = owner?.username,
            capacity = capacity?.volume,
            currentVolume = currentVolume
    )
}
