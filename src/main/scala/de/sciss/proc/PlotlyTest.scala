package de.sciss.proc

import com.raquo.laminar.api.L.{documentEvents, unsafeWindowOwner}
import plotly.Plotly._
import plotly._
import plotly.layout.Layout

object PlotlyTest {
  def run(): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      runGUI()
    }(unsafeWindowOwner)
  }

  def runGUI(): Unit = {
    val x = (0 to 100).map(_ * 0.1)
    val y1 = x.map(d => 2.0 * d + util.Random.nextGaussian())
    val y2 = x.map(math.exp)

    val plot = Seq(
      Scatter(x, y1).withName("Approx twice"),
      Scatter(x, y2).withName("Exp"),
    )

    val lay = Layout().withTitle("Curves")
    plot.plot("plot", lay)
  }
}
