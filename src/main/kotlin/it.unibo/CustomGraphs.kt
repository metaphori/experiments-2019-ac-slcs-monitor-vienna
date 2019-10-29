package it.unibo

import it.unibo.alchemist.model.interfaces.Node
import org.jgrapht.graph.DefaultListenableGraph

class CustomDefaultListenableGraph<V>(val g: G<V>, val name: String = "untitled") : DefaultListenableGraph<Node<V>,TEdge<V>>(g) {
    // Let's optimise getting edge source and target
    override fun getEdgeSource(e: TEdge<V>): Node<V> = e.first
    override fun getEdgeTarget(e: TEdge<V>): Node<V> = e.second
}