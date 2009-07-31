/* Poly2Tri
 * Copyright (c) 2009, Mason Green
 * http://code.google.com/p/poly2tri/
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Poly2Tri nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.poly2tri

import org.newdawn.slick.{BasicGame, GameContainer, Graphics, Color, AppGameContainer}
import org.newdawn.slick.geom.{Polygon, Circle}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import seidel.Triangulator
import shapes.{Segment, Point, Triangle}
import earClip.EarClip
import cdt.CDT

// TODO: Lots of documentation!

object Poly2Tri {

  def main(args: Array[String]) {
	val container = new AppGameContainer(new Poly2TriDemo())
    container.setDisplayMode(800,600,false)
    container.start()
  }
  
}

class Poly2TriDemo extends BasicGame("Poly2Tri") {
  
  // Sedidel Triangulator
  var seidel: Triangulator = null
  var segments: ArrayBuffer[Segment] = null
  
  // EarClip Triangulator
  val earClip = new EarClip
  var earClipResults: Array[poly2tri.earClip.Triangle] = null
  
  // Sweep Line Constraied Delauney Triangulator (CDT)
  var slCDT: CDT = null
  
  var polyX: ArrayBuffer[Float] = null
  var polyY: ArrayBuffer[Float] = null
    
  var quit = false
  var debug = false
  var drawMap = false
  var drawSegs = true
  var hiLighter = 0
  var drawEarClip = false
  var hertelMehlhorn = false
  var drawCDT = false
  
  val nazcaMonkey = "data/nazca_monkey.dat"
  val bird = "data/bird.dat"
  val snake = "data/i.snake"
  val star = "data/star.dat"
  val strange = "data/strange.dat"
  val i18 = "data/i.18"
  
  var currentModel = nazcaMonkey
  
  def init(container: GameContainer) {
    selectModel(currentModel)
  }
  
  def update(gc: GameContainer, delta: Int) {
    if(quit) gc exit
  }
  
  def render(container: GameContainer, g: Graphics) {
    
    g.drawString("'1-6' to cycle models", 10, 540)
    g.drawString("'SPACE' to turn on debug info", 10, 552)
    g.drawString("'m' to show trapezoidal map (debug mode)", 10, 564)
    g.drawString("'e' to switch Seidel / EarClip", 10, 576)
    
    val red = new Color(1f, 0f,0.0f)
    val blue = new Color(0f, 0f, 1f)
    val green = new Color(0f, 1f, 0f)
    val yellow = new Color(1f, 1f, 0f)
    
   if(debug) {
	   val draw = if(drawMap) seidel.trapezoidMap else seidel.trapezoids
	   for(t <- draw) {
	     val polygon = new Polygon()
	     for(v <- t.vertices) {
	       polygon.addPoint(v.x, v.y)
	     }
	     if(!drawMap) {
	       val lCirc = new Circle(t.leftPoint.x, t.leftPoint.y, 4)
	       g.setColor(blue); g.draw(lCirc); g.fill(lCirc)
	       val rCirc = new Circle(t.rightPoint.x+5, t.rightPoint.y, 4)
	       g.setColor(yellow); g.draw(rCirc); g.fill(rCirc)
         }                          
	     g.setColor(red)
	     g.draw(polygon) 
	    }
   }
   
   if(!debug && !drawEarClip && !drawCDT) {
    var i = 0
    for(t <- seidel.polygons) {
        val poly = new Polygon
        t.foreach(p => poly.addPoint(p.x, p.y))
        g.setColor(red)
        g.draw(poly)
      }
   } else if (debug && drawMap && !drawEarClip){
    for(mp <- seidel.monoPolies) {
      val poly = new Polygon
      mp.foreach(p => poly.addPoint(p.x, p.y))
      g.setColor(yellow)
      g.draw(poly)
      }
    }
   
   if(drawEarClip) 
	   earClipResults.foreach(t => {
	      val triangle = new Polygon
	      triangle.addPoint(t.x(0), t.y(0))
	      triangle.addPoint(t.x(1), t.y(1))
	      triangle.addPoint(t.x(2), t.y(2))
	      g.setColor(red)
	      g.draw(triangle)
	    })
   
   if(drawCDT) {
	   slCDT.mesh.map.foreach( t => {
	     val triangle = new Polygon
		 triangle.addPoint(t.points(0).x, t.points(0).y)
		 triangle.addPoint(t.points(1).x, t.points(1).y)
		 triangle.addPoint(t.points(2).x, t.points(2).y)
		 g.setColor(red)
		 g.draw(triangle) 
	   })
   }
   
   if(drawSegs) {
     g.setColor(green)
     for(i <- 0 until segments.size) {
       val s = segments(i)
       g.drawLine(s.p.x,s.p.y,s.q.x,s.q.y)
     }
   }
   
  }
  
  override def keyPressed(key:Int, c:Char) {
    
    // ESC
    if(key == 1) quit = true
    // SPACE
    if(key == 57) debug = !debug
    // UP
    if(key == 200) {
      hiLighter += 1
      if (hiLighter == seidel.polygons.size)
        hiLighter = 0
    }
    // DOWN
    if(key == 208) {
      hiLighter -= 1
      if (hiLighter == -1)
        hiLighter = seidel.polygons.size-1
    }
    if(c == 'm') drawMap = !drawMap 
    //if(c == 'c') drawCDT = !drawCDT
    if(c == '1') selectModel(nazcaMonkey)
    if(c == '2') selectModel(bird)
    if(c == '3') selectModel(strange)
    if(c == '4') selectModel(snake)
    if(c == '5') selectModel(star)
    if(c == '6') selectModel(i18)
    if(c == 's') drawSegs = !drawSegs
    if(c == 'e') {drawEarClip = !drawEarClip; selectModel(currentModel)}
    if(c == 'h') {hertelMehlhorn = !hertelMehlhorn; selectModel(currentModel)} 
  }
    
  def selectModel(model: String) {
    model match {
      case "data/nazca_monkey.dat" => 
        loadModel(nazcaMonkey, 4.5f, Point(400, 300), 1500)
      case "data/bird.dat" => 
        loadModel(bird, 25f, Point(400, 300), 350)
      case "data/i.snake" => 
        loadModel(snake, 10f, Point(600, 300), 10)
      case "data/star.dat" => 
        loadModel(star, -1f, Point(0f, 0f), 10)
      case "data/strange.dat" => 
        loadModel(strange, -1f, Point(0f, 0f), 15)
      case "data/i.18" => 
        loadModel(i18, 20f, Point(600f, 500f), 20)
      case _ => 
        assert(false)
    }
    currentModel = model
  }
   
  def loadModel(model: String, scale: Float, center: Point, maxTriangles: Int) {
    
    println("*** " + model + " ***")
    
    polyX = new ArrayBuffer[Float]
    polyY = new ArrayBuffer[Float]
    val points = new ArrayBuffer[Point]
    
    val angle = Math.Pi
    for (line <- Source.fromFile(model).getLines) {
      val s = line.replaceAll("\n", "")
      val tokens = s.split("[ ]+")
      if(tokens.size == 2) {	
       var x = tokens(0).toFloat
       var y = tokens(1).toFloat
       // Transform the shape
       polyX += (Math.cos(angle)*x - Math.sin(angle)*y).toFloat * scale + center.x
       polyY += (Math.sin(angle)*x + Math.cos(angle)*y).toFloat * scale + center.y
       points += new Point(polyX.last, polyY.last)
      } else {
        throw new Exception("Bad input file")
      }
    }
    
    segments = new ArrayBuffer[Segment]
    for(i <- 0 until polyX.size-1)
      segments += new Segment(points(i), points(i+1))
    segments += new Segment(points.first, points.last)
    
    //slCDT = CDT.init(points)
    
    println("Number of points = " + polyX.size)
    println
     
    if(!drawEarClip) {  
      
	    // Sediel triangulation
	    seidel = new Triangulator(segments)
        seidel.buildTriangles = hertelMehlhorn
        
	    val t1 = System.nanoTime
	    seidel.process
	    val runTime = System.nanoTime - t1
	
	    println("Poly2Tri average (ms) =  " + runTime*1e-6)
	    println("Number of triangles = " + seidel.polygons.size)
     
    } else {
      
    	// Earclip
	    
	    earClipResults = new Array[poly2tri.earClip.Triangle](maxTriangles)
     
	    for(i <- 0 until earClipResults.size) earClipResults(i) = new poly2tri.earClip.Triangle
     
	    var xVerts = polyX.toArray
	    var yVerts = polyY.toArray
       
        if(currentModel != strange) {
	      xVerts = xVerts.reverse
          yVerts = yVerts.reverse
        }
	   
	    val t1 = System.nanoTime
	    earClip.triangulatePolygon(xVerts, yVerts, xVerts.size, earClipResults)
	    val runTime = System.nanoTime - t1
	
	    println
	    println("Earclip average (ms) =  " + runTime*1e-6) 
	    println("Number of triangles = " + earClip.numTriangles)
    }
  }
   
}