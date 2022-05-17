package matt.v1.model.complexcell

import matt.kjlib.jmath.point.Point
import matt.v1.low.PhaseType.COS
import matt.v1.low.PhaseType.SIN
import matt.v1.model.Cell
import matt.v1.model.SimpleCell


data class ComplexCell(
  val sinCell: SimpleCell<SIN>, val cosCell: SimpleCell<COS>
): Cell by sinCell, Point by sinCell {
  constructor(cells: Pair<SimpleCell<SIN>, SimpleCell<COS>>): this(sinCell = cells.first, cosCell = cells.second)
}