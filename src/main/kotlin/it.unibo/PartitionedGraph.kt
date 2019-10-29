package it.unibo

import it.unibo.alchemist.model.implementations.environments.AbstractEnvironment
import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Position
import org.jgrapht.graph.DefaultListenableGraph
import org.jgrapht.graph.builder.GraphTypeBuilder

class PartitionedGraph<V,P : Position<P>>(val env: Environment<V, P>,
                                          val partitionProperty: (Node<V>) -> Boolean) :
        DefaultListenableGraph<Node<V>, TEdge<V>>(GraphTypeBuilder.undirected<Node<V>, TEdge<V>>().buildGraph()),
        NodeObserver,
        NeighbourhoodObserver<V, P> {

    val gtrue: G<V> = CustomDefaultListenableGraph(GraphTypeBuilder.undirected<Node<V>, TEdge<V>>().buildGraph(), "gtrue")
    val gfalse: G<V> = CustomDefaultListenableGraph(GraphTypeBuilder.undirected<Node<V>, TEdge<V>>().buildGraph(), "gfalse")

    val gtrueInspector = CustomConnectivityInspector<Node<V>, TEdge<V>>(gtrue)
    val gfalseInspector = CustomConnectivityInspector<Node<V>, TEdge<V>>(gfalse)

    init {
        (env as AbstractEnvironment).addObserver(this)
        (gtrue as LG<V>).addGraphListener(gtrueInspector)
        (gfalse as LG<V>).addGraphListener(gfalseInspector)
        updateConnectivityInspectors()
    }

    constructor(env: Environment<V, P>, partitionProperty: (Node<V>) -> Boolean, g: G<V>) : this(env, partitionProperty) {
        g.vertexSet().forEach {
            this.addVertex(it)
            if (it is ProtelisNode<*>) (it as ProtelisNode<*>).addObserver(this)
        }
        g.edgeSet().forEach {
            this.addEdge(it.first, it.second, it)
        }
        updateConnectivityInspectors()
    }

    fun updateConnectivityInspectors(){
        gtrueInspector.connectedSets()
        gfalseInspector.connectedSets()
    }

    fun graphsForNode(v: Node<V>): Pair<G<V>,G<V>> {
        return if (partitionProperty(v)) Pair(gtrue,gfalse) else Pair(gfalse,gtrue)
    }

    override fun addVertex(v: Node<V>): Boolean {
        val (corrG,_) = graphsForNode(v)
        corrG.addVertex(v)
        return super.addVertex(v)
    }

    override fun removeVertex(v: Node<V>?): Boolean {
        val r = super.removeVertex(v)
        gtrue.removeVertex(v)
        gfalse.removeVertex(v)
        return r
    }

    override fun addEdge(sourceVertex: Node<V>, targetVertex: Node<V>, e: TEdge<V>): Boolean {
        val (from, to) = e
        if (gtrue.containsVertex(from) && gtrue.containsVertex(to)) gtrue.addEdge(from, to, e)
        else if (gfalse.containsVertex(from) && gfalse.containsVertex(to)) gfalse.addEdge(from, to, e)
        return super.addEdge(sourceVertex, targetVertex, e)
    }

    override fun removeEdge(e: TEdge<V>): Boolean {
        val r = super.removeEdge(e)
        gtrue.removeEdge(e.first, e.second)
        gfalse.removeEdge(e.first, e.second)
        return r
    }

    override fun removeEdge(sourceVertex: Node<V>, targetVertex: Node<V>): TEdge<V> {
        val e = Pair(sourceVertex, targetVertex)
        removeEdge(e)
        return e
    }

    var i = 0
    override fun <T> notifyEvent(ev: NodeObserverEvent<T>) {
        when(ev){
            is NodeObserverEvent.ConcentrationChange -> {
                // println("${ev.node.id} changes ${ev.mol.name} from ${ev.past} to ${ev.newValue}")
                try {
                    // Update graph partitions accordingly
                    val (newg, oldg) = graphsForNode(ev.node as Node<V>)
                    newg.addVertex(ev.node)
                    env.getNeighborhood(ev.node).forEach {
                        if (newg.containsVertex(it)) newg.addEdge(ev.node, it, Pair(ev.node, it))
                    }
                    oldg.removeVertex(ev.node)
                } catch(e: Exception){ e.printStackTrace() }
            }
            else -> println("Node event not recognised")
        }
    }

    override fun notifyEvent(ev: NeighbourhoodObserverEvent<V, P>) {
        when(ev){
            is NeighbourhoodObserverEvent.NeighbourhoodChange -> try {
                // println("Neighbour change: add? ${ev.isAdd} - ${ev.origin} -> ${ev.dest}")
                if (ev.isAdd) {
                    this.addEdge(ev.origin, ev.dest, Pair(ev.origin, ev.dest))
                } else {
                    this.removeEdge(ev.origin, ev.dest)
                }
            } catch(e: Exception){ e.printStackTrace() }
            else -> println("Neighbourhood event not recognised")
        }
    }

    // Let's optimise getting edge source and target
    override fun getEdgeSource(e: TEdge<V>): Node<V> = e.first
    override fun getEdgeTarget(e: TEdge<V>): Node<V> = e.second
}