package gemmini

import chisel3._

case class InterconnectConfig[T <: Data : Arithmetic](
  verticalGridType : T,
  horizontalGridType : T,
  interPEType : T,
  val allowed_sel_a: Seq[PEMuxSel.Type],
  val allowed_sel_b: Seq[PEMuxSel.Type],
  val allowed_sel_c: Seq[PEMuxSel.Type],
  val allowed_sel_q: Seq[PEMuxSel.Type]
)

object CommonInterconnectConfigs {
  // Dataflow direction of the unmodified Gemmini
  val DefaultICConfig = InterconnectConfig[SInt](
    verticalGridType = SInt(32.W),
    horizontalGridType = SInt(8.W),
    interPEType = SInt(32.W),
    allowed_sel_a = Seq(PEMuxSel.V_GRID, PEMuxSel.H_GRID, PEMuxSel.H, PEMuxSel.D, PEMuxSel.ZERO, PEMuxSel.IDENTITY),
    allowed_sel_b = Seq(PEMuxSel.V_GRID, PEMuxSel.V, PEMuxSel.REG, PEMuxSel.ZERO, PEMuxSel.IDENTITY),
    allowed_sel_c = Seq(PEMuxSel.V, PEMuxSel.H, PEMuxSel.D, PEMuxSel.REG, PEMuxSel.ZERO, PEMuxSel.IDENTITY),
    allowed_sel_q = Seq(PEMuxSel.V, PEMuxSel.H, PEMuxSel.D),
  )
}