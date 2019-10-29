package it.unibo.alchemist.loader.displacements

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Position

open class SpecificPositions(
        environment: Environment<*, *>,
        vararg positions: Iterable<Number>
) : Displacement<Position<*>> {

    private val positions: List<Position<*>> = positions.map { environment.makePosition(*it.toList().toTypedArray()) }

    override fun stream() = positions.stream()
}