package no.eolseng.pgr301examauth.beer

import no.eolseng.pgr301examauth.db.User
import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

interface MugRepository : JpaRepository<Mug, Int>

@Entity
class Mug(

        @get:Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Int? = null,

        @get:NotNull
        @get:ManyToOne
        var owner: User? = null,

        @get:NotNull
        var capacity: Size? = null,

        @get:Min(0)
        @get:Max(6)
        var currentVolume: Int? = null

) {
    enum class Size(val volume: Int) {
        SMALL(3),
        MEDIUM(4),
        LARGE(5),
        HUGE(6)
    }
}

data class MugDto(
        val id: Int? = null,
        val ownerName: String? = null,
        @get:Size(min = 3, max = 6,
                message = "Keg size must be between 3 dl and 6 dl.")
        val capacity: Int? = null,

        @get:Size(min = 0, max = 6,
                message = "Volume must cannot be negative and can not be over 600 dl.")
        val currentVolume: Int? = null
)

fun Mug.toDto(): MugDto {
    return MugDto(
            id = id,
            ownerName = owner?.username,
            capacity = capacity?.volume,
            currentVolume = currentVolume
    )
}
