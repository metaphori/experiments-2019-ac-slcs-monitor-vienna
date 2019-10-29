@file:JvmName("SimulationUtils")

package it.unibo

import com.google.common.cache.CacheBuilder
import it.unibo.alchemist.model.implementations.environments.AbstractEnvironment
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces.*
import org.jgrapht.Graph
import org.jgrapht.Graphs
import org.jgrapht.ListenableGraph
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DefaultListenableGraph
import org.jgrapht.graph.DefaultUndirectedGraph
import org.jgrapht.graph.builder.GraphTypeBuilder

val MIN_DISTANCE_TO_DANGEROUS_POINTS = 100

val graphCache = CacheBuilder.newBuilder().weakKeys().build<Environment<*,*>,SGraphs<*,*>>()

typealias TEdge<T> = Pair<Node<T>,Node<T>>
typealias G<T> = Graph<Node<T>, TEdge<T>>
typealias LG<T> = ListenableGraph<Node<T>, TEdge<T>>
typealias SubG<T> = AsSubgraph<Node<T>,TEdge<T>>
typealias GListener<T> = CustomConnectivityInspector<Node<T>,TEdge<T>>

interface NodeObserver {
    fun <T> notifyEvent(source: NodeObserverEvent<T>)
}

open class NodeObserverEvent<T>(val node: Node<T>) { // private constructor to prevent creating more subclasses outside
    class ConcentrationChange<T>(node: Node<T>, val mol: Molecule, val past: Any?, val newValue: Any?) : NodeObserverEvent<T>(node)
}

interface NeighbourhoodObserver<T, P : Position<P>> {
    fun notifyEvent(source: NeighbourhoodObserverEvent<T,P>)
}

open class NeighbourhoodObserverEvent<T,P : Position<P>>(val env: Environment<T,P>) { // private constructor to prevent creating more subclasses outside
    class NeighbourhoodChange<T,P : Position<P>>(env: Environment<T,P>, val origin: Node<T>, val dest: Node<T>, val isAdd: Boolean) : NeighbourhoodObserverEvent<T,P>(env)
}

data class SGraphs<T,P:Position<P>>(
        val pg: PartitionedGraph<T,P>,
        val molecules: MutableMap<String,SimpleMolecule> = mutableMapOf())

fun <T, P : Position<P>> getMoleculeByName(env: Environment<T,P>, name: String): SimpleMolecule =
        graphForEnv(env).molecules.getOrPut(name) { println("Create molecule $name"); SimpleMolecule(name) }

fun <T, P : Position<P>> graphForEnv(env: Environment<T,P>): SGraphs<T,P> {
    if(!graphCache.asMap().containsKey(env)){
        println("Building graph for environments ${env}")
        val g: G<T> = buildGraphFromEnv(env)

        println("Registering observed molecules")
        (env as AbstractEnvironment<T,P>).observedMolecules.add(getDangerousMoleculeName(env))

        val newg: G<T> = DefaultUndirectedGraph({ null }, { null }, false)
        Graphs.addGraph(newg, g as G<T>)

        val m = SimpleMolecule(getDangerousMoleculeName(env))
        val pg = PartitionedGraph<T,P>(env, {
            it.getConcentration(m)==true
        }, g)

        val gs =  SGraphs(pg)
        graphCache.put(env, gs)
        return gs
    } else {
        return graphCache.getIfPresent(env) as SGraphs<T,P>
    }
}

fun <P: Position<P>> getDangerousMoleculeName(env: Environment<*,P>): String {
    return env.layers.get(0).getValue(null) as String
}

data class Quadruple<A,B,C,D>(val a: A, val b:B, val c:C, val d:D)

fun <T, P : Position<P>> monitoringPropertySatisfied(env: Environment<T, P>,
                                                     node: Int): Boolean {
    var graphs = graphForEnv(env)

    val currNode = env.getNodeByID(node)
    val dangerousMol = getMoleculeByName(env,env.layers.get(0).getValue(null) as String)
    val safePlaceMol = getMoleculeByName(env,env.layers.get(1).getValue(null) as String)

    val dangerousSubgraph = graphs.pg.gtrue
    val nonDangerousSubgraph = graphs.pg.gfalse
    val dangerousInspector = graphs.pg.gtrueInspector
    val nonDangerousInspector = graphs.pg.gfalseInspector

    if(currNode.getConcentration(dangerousMol)==true && dangerousSubgraph.containsVertex(currNode)){
        return dangerousInspector.connectedSetOf(currNode).all {
            env.getNeighborhood(it).filter { nonDangerousSubgraph.containsVertex(it) }.all {
                nonDangerousInspector.connectedSetOf(it).any{ it.getConcentration(safePlaceMol)==true }
            }
        }
    } else {
        return nonDangerousInspector.connectedSetOf(currNode).any{ it.getConcentration(safePlaceMol)==true }
    }
}

fun <T, P:Position<P>> buildGraphFromEnv(env: Environment<T, P>): G<T> {
    val g  = DefaultListenableGraph(GraphTypeBuilder.undirected<Node<T>, TEdge<T>>().buildGraph())

    // val g = GraphTypeBuilder.undirected<Node<T>, Edge<T>>().allowingMultipleEdges(false).allowingSelfLoops(true).edgeClass(Pair::class.java).weighted(false).buildGraph();
    env.nodes.forEach { node ->
        g.addVertex(node)
        env.getNeighborhood(node).neighbors.forEach{nbr ->
            if(!g.containsVertex(nbr)) g.addVertex(nbr)
            g.addEdge(node, nbr, Pair(node,nbr))
        }
    }

    return g
}


fun <T, P : Position<P>> checkGraph(env: Environment<T, P>, node: Int): Unit {
    var gs = graphForEnv(env).pg
    println("GTRUE: ${gs.gtrue}\nGFALSE: ${gs.gfalse}\n-------------------------------------")
    System.out.flush()
}

fun <T, P : Position<P>> getPos(env: Environment<T, P>, node: Int): P {
    return env.getPosition(env.getNodeByID(node))
}
