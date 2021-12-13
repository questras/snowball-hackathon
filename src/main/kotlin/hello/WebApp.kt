/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hello

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.Micronaut
import io.reactivex.Single

fun main() {
    Micronaut.run(WebApp::class.java)
}

data class Point(val x: Int, val y: Int)

data class Self(val href: String)

data class Links(val self: Self)

data class PlayerState(val x: Int, val y: Int, val direction: String, val wasHit: Boolean, val score: Int)

data class Arena(val dims: List<Int>, val state: Map<String, PlayerState>)

data class ArenaUpdate(val _links: Links, val arena: Arena)

fun getHitPoints(x: Int, y: Int, xChange: Int, yChange: Int): List<Point> {
    return listOf(
        Point(x + xChange*1, y + yChange * 1),
        Point(x + xChange*2, y + yChange * 2),
        Point(x + xChange*3, y + yChange * 3)
    )
}

fun getHitLine(point: Point, direction: String): List<Point> {
    return if (direction == "N") {
        getHitPoints(point.x, point.y, 0, -1)
    }
    else if (direction == "E") {
        getHitPoints(point.x, point.y, 1, 0)
    }
    else if (direction == "S") {
        getHitPoints(point.x, point.y, 0, 1)
    }
    else {
        getHitPoints(point.x, point.y, -1, 0)
    }
}

fun canHit(myLink: String, arena: Arena): Boolean {
    val myState = arena.state[myLink]!!
    val hitPoints = getHitLine(Point(myState.x, myState.y), myState.direction).toSet()

    for (name in arena.state.keys) {
        val pState = arena.state[name]!!
        if (hitPoints.contains(Point(pState.x, pState.y))) {
            return true
        }
    }

    return false
}

fun willBeHit(point: Point, arena: Arena, hasPlayerByPoint: Map<Point, Boolean>): Boolean {
    // to the right need to have W
    // to the left need to have E
    // to the Top need to have S
    // to the bottom need to have N

    val rightPlayers = getHitLine(point, "W").filter { hasPlayerByPoint[it]!! }
    val leftPlayers = getHitLine(point, "E").filter { hasPlayerByPoint[it]!! }
    val topPlayers = getHitLine(point, "S").filter { hasPlayerByPoint[it]!! }
    val bottomPlayers = getHitLine(point, "N").filter { hasPlayerByPoint[it]!! }
    return true

}

// If my position is hit then I try to move in direction that is not hit
// pseudocode:
// if not my position is in hit and canHit then THROW
// else
//.
//  if forward is not hit then FORWARD
//. else

fun strategy(arenaUpdate: ArenaUpdate): String {
    val myLink = arenaUpdate._links.self.href
    return ""
}

fun populateMap(arena: Arena): MutableMap<Point, Boolean> {
    val width = arena.dims[0]
    val height = arena.dims[1]

    val m = mutableMapOf<Point, Boolean>()
    for (x in 0 until width) {
        for (y in 0 until height) {
            val currentPoint = Point(x, y)
            for (k in arena.state.keys) {
                if (arena.state[k]!!.x == currentPoint.x && arena.state[k]!!.y == currentPoint.y) {
                    m[currentPoint] = true
                }
            }

            m[currentPoint] = m.contains(currentPoint)
        }
    }

    return m
}

fun getMoveAction(arena: Arena): String {
    return listOf("R", "L", "F").random()
}

@Controller
class WebApp {

    @Get
    fun index() = run {
        "Let the battle begin!"
    }

    @Post(uris = ["/", "/{+path}"])
    fun index(@Body maybeArenaUpdate: Single<ArenaUpdate>): Single<String> {
        return maybeArenaUpdate.map { arenaUpdate ->
            val myLink = arenaUpdate._links.self.href
            val myState = arenaUpdate.arena.state[myLink]!!
            val myPoint = Point(myState.x, myState.y)

            val hasPlayerByPoint = populateMap(arenaUpdate.arena)


            if (canHit(myLink, arenaUpdate.arena) && !myState.wasHit) {
                "T"
            }
            else {
                getMoveAction(arenaUpdate.arena)
            }
        }
    }

}
