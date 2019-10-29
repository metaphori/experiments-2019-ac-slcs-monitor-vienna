package it.unibo

import org.jgrapht.Graph
import org.jgrapht.event.*
import org.jgrapht.graph.AsUndirectedGraph
import org.jgrapht.graph.DefaultListenableGraph
import org.jgrapht.traverse.BreadthFirstIterator
import java.util.*

/**
 * Allows obtaining various connectivity aspects of a graph. The *inspected graph* is specified
 * at construction time and cannot be modified. Currently, the inspector supports connected
 * components for an undirected graph and weakly connected components for a directed graph. To find
 * strongly connected components, use [KosarajuStrongConnectivityInspector] instead.
 *
 * The inspector methods work in a lazy fashion: no computation is performed unless immediately
 * necessary. Computation are done once and results and cached within this class for future need.
 *
 * The inspector is also a [org.jgrapht.event.GraphListener]. If added as a listener to the
 * inspected graph, the inspector will amend internal cached results instead of recomputing them. It
 * is efficient when a few modifications are applied to a large graph. If many modifications are
 * expected it will not be efficient due to added overhead on graph update operations. If inspector
 * is added as listener to a graph other than the one it inspects, results are undefined.
 *
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Barak Naveh
 * @author John V. Sichi
</E></V> */
class CustomConnectivityInspector<V, E>(g: Graph<V, E>) : GraphListener<V, E> {
    private var connectedSets: MutableList<MutableSet<V>>? = null
    private var vertexToConnectedSet: MutableMap<V, MutableSet<V>?> = mutableMapOf()
    private var graph: Graph<V, E> = g

    /**
     * Test if the inspected graph is connected. A graph is connected when there is a path between
     * every pair of vertices. In a connected graph, there are no unreachable vertices. When the
     * inspected graph is a *directed* graph, this method returns true if and only if the
     * inspected graph is *weakly* connected. An empty graph is *not* considered
     * connected.
     *
     * @return `true` if and only if inspected graph is connected.
     */
    val isConnected: Boolean
        get() = lazyFindConnectedSets().size == 1

    init {
        init()
        this.graph = Objects.requireNonNull(g)
        if (g.type.isDirected)
            this.graph = AsUndirectedGraph(g)
    }

    /**
     * Returns a set of all vertices that are in the maximally connected component together with the
     * specified vertex. For more on maximally connected component, see
     * [
 * http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html](http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html).
     *
     * @param vertex the vertex for which the connected set to be returned.
     *
     * @return a set of all vertices that are in the maximally connected component together with the
     * specified vertex.
     */
    fun connectedSetOf(vertex: V): Set<V> {
        var connectedSet: MutableSet<V>? = vertexToConnectedSet.getOrDefault(vertex, null)

        if (connectedSet == null) {
            connectedSet = HashSet()

            val i = BreadthFirstIterator(graph!!, vertex)

            while (i.hasNext()) {
                connectedSet.add(i.next())
            }

            vertexToConnectedSet!![vertex] = connectedSet
        }

        return connectedSet
    }

    /**
     * Returns a list of `Set` s, where each set contains all vertices that are in the
     * same maximally connected component. All graph vertices occur in exactly one set. For more on
     * maximally connected component, see
     * [
 * http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html](http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html).
     *
     * @return Returns a list of `Set` s, where each set contains all vertices that are
     * in the same maximally connected component.
     */
    fun connectedSets(): List<Set<V>> {
        return lazyFindConnectedSets()
    }

    /**
     * Tests whether two vertices lay respectively in the same connected component (undirected
     * graph), or in the same weakly connected component (directed graph).
     *
     * @param sourceVertex one end of the path.
     * @param targetVertex another end of the path.
     *
     * @return `true` if and only if the source and target vertex are in the same
     * connected component (undirected graph), or in the same weakly connected component
     * (directed graph).
     */
    fun pathExists(sourceVertex: V, targetVertex: V): Boolean {
        return connectedSetOf(sourceVertex).contains(targetVertex)
    }

    /**
     * @see VertexSetListener.vertexAdded
     */
    override fun vertexAdded(e: GraphVertexChangeEvent<V>) {
        // println("[${this}] Vertex ${e.vertex} added to ${(graph as CustomDefaultListenableGraph<V>).name}")

        val component = HashSet<V>()
        component.add(e.vertex)
        if(connectedSets==null) connectedSets = mutableListOf()
        connectedSets!!.add(component)
        vertexToConnectedSet!![e.vertex] = component
    }

    /**
     * @see GraphListener.edgeAdded
     */
    override fun edgeAdded(e: GraphEdgeChangeEvent<V, E>) {
        // println("[${this}] Edge ${e.edgeSource}->${e.edgeTarget} added to ${(graph as CustomDefaultListenableGraph<V>).name}")
        val source = e.edgeSource
        val target = e.edgeTarget
        val sourceSet = connectedSetOf(source)
        val targetSet = connectedSetOf(target)

        // If source and target are in the same set, do nothing, otherwise, merge sets
        if (sourceSet !== targetSet) {
            val merge = HashSet<V>()
            merge.addAll(sourceSet)
            merge.addAll(targetSet)
            if(connectedSets==null) connectedSets = mutableListOf()
            connectedSets!!.remove(sourceSet)
            connectedSets!!.remove(targetSet)
            connectedSets!!.add(merge)
            for (v in merge)
                vertexToConnectedSet!![v] = merge
        }
    }

    /**
     * @see VertexSetListener.vertexRemoved
     */
    override fun vertexRemoved(e: GraphVertexChangeEvent<V>) {
        // println("[${this}] Vertex ${e.vertex} removed from ${(graph as CustomDefaultListenableGraph<V>).name}")

        init() // for now invalidate cached results, in the future need to
        // amend them. If the vertex is an articulation point, two
        // components need to be split

    }

    /**
     * @see GraphListener.edgeRemoved
     */
    override fun edgeRemoved(e: GraphEdgeChangeEvent<V, E>) {
        // println("[${this}] Edge ${e.edgeSource}->${e.edgeTarget} removed from ${(graph as CustomDefaultListenableGraph<V>).name}")

        init() // for now invalidate cached results, in the future need to
        // amend them. If the edge is a bridge, 2 components need to be split.

        // MUST BE DONE LAZILY (IF SOMEONE ASKS FOR CONNECTED COMPONENT OF source or target)

        /*
        //val (fromComp,toComp) = Pair(vertexToConnectedSet.get(e.edgeSource), vertexToConnectedSet.get(e.edgeTarget))
        vertexToConnectedSet.put(e.edgeSource, null)
        vertexToConnectedSet.put(e.edgeTarget, null)
        val c1 = connectedSetOf(e.edgeSource)
        val c2 = connectedSetOf(e.edgeTarget)
        if(setDisjoint(c1,c2)) {
            c1.stream().filter { vertexToConnectedSet.get(it) != null }.forEach {
                vertexToConnectedSet.get(it)!!.removeAll(c2)
            }
            c2.stream().filter { vertexToConnectedSet.get(it) != null }.forEach {
                vertexToConnectedSet.get(it)!!.removeAll(c1)
            }
        }
         */
    }

    fun <T> setDisjoint(s: Set<T>, t: Set<T>): Boolean {
        return s.intersect(t).isEmpty()
    }

    private fun init() {
        connectedSets = null
        vertexToConnectedSet = HashMap()
    }

    private fun lazyFindConnectedSets(): MutableList<MutableSet<V>> {
        if (connectedSets == null) {
            connectedSets = ArrayList()

            val vertexSet = graph!!.vertexSet()

            if (!vertexSet.isEmpty()) {
                val i = BreadthFirstIterator(graph!!)
                i.addTraversalListener(MyTraversalListener())

                while (i.hasNext()) {
                    i.next()
                }
            }
        }

        return connectedSets!!
    }

    /**
     * A traversal listener that groups all vertices according to to their containing connected set.
     *
     * @author Barak Naveh
     */
    private inner class MyTraversalListener : TraversalListenerAdapter<V, E>() {
        private var currentConnectedSet: MutableSet<V>? = null

        /**
         * @see TraversalListenerAdapter.connectedComponentFinished
         */
        override fun connectedComponentFinished(e: ConnectedComponentTraversalEvent?) {
            connectedSets!!.add(currentConnectedSet!!)
        }

        /**
         * @see TraversalListenerAdapter.connectedComponentStarted
         */
        override fun connectedComponentStarted(e: ConnectedComponentTraversalEvent?) {
            currentConnectedSet = HashSet()
        }

        /**
         * @see TraversalListenerAdapter.vertexTraversed
         */
        override fun vertexTraversed(e: VertexTraversalEvent<V>) {
            val v = e.vertex
            currentConnectedSet!!.add(v)
            vertexToConnectedSet!![v] = currentConnectedSet!!
        }
    }
}