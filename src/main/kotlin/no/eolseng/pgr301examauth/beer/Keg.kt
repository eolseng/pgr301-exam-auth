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

        @get:Min(10)
        @get:Max(600)
        var capacity: Int = 10,

        @get:Min(0)
        @get:Max(600L)
        var currentVolume: Int = 0

)

data class KegDto(
        val id: Int? = null,
        val ownerName: String? = null,
        @get:Min(10, message = "Keg size must be between 10 dl and 600 dl.")
        @get:Max(600, message = "Keg size must be between 10 dl and 600 dl.")
        val capacity: Int? = null,

        @get:Min(0, message = "Volume cannot be negative")
        @get:Max(600, message = "Volume cannot be bigger than max keg size (600 dl)")
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