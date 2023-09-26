// See README.md for license details.
package gemmini

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class ModPEControl[T <: Data/* : Arithmetic*/](interconnectConfig : InterconnectConfig[T]) extends Bundle {
  val sel_a = PEMuxSel()
  val sel_b = PEMuxSel()
  val sel_c = PEMuxSel()
  val sel_q = PEMuxSel()
  val fu_control = new FUControl(interconnectConfig)
  val sel_out_v_grid = OutGridMuxSel()
  val sel_out_h_grid = OutGridMuxSel()
  val sel_out = OutMuxSel()
  val reg_p_en = Bool()
  val reg_q_en = Bool()
  val double_buffer_swap = Bool()
}

object PEMuxSel extends ChiselEnum {
  val V_GRID   = Value("b000".U)
  val V        = Value("b001".U)
  val H_GRID   = Value("b010".U)
  val H        = Value("b011".U)
  val D        = Value("b100".U)
  val REG      = Value("b101".U)
  val ZERO     = Value("b110".U)
  val IDENTITY = Value("b111".U)
}

object OutGridMuxSel extends ChiselEnum {
  val FORWARD, FU = Value
}

object OutMuxSel extends ChiselEnum {
  val FU, REG = Value
}

class ModPE[T <: Data](interconnectConfig : InterconnectConfig[T])
                   (implicit ev: Arithmetic[T]) extends Module { // Debugging variables
  import ev._

  val io = IO(new Bundle {
    val in_v_grid = Input(interconnectConfig.verticalGridType)
    val in_h_grid = Input(interconnectConfig.horizontalGridType)
    val out_v_grid = Output(interconnectConfig.verticalGridType)
    val out_h_grid = Output(interconnectConfig.horizontalGridType)
    val in_v = Input(interconnectConfig.interPEType)
    val in_h = Input(interconnectConfig.interPEType)
    val in_d = Input(interconnectConfig.interPEType)
    val out = Output(interconnectConfig.interPEType)

    val control = Input(new ModPEControl(interconnectConfig))
    val valid = Input(Bool())
  })

  val sel_a = Wire(PEMuxSel())
  val sel_b = Wire(PEMuxSel())
  val sel_c = Wire(PEMuxSel())
  val sel_q = Wire(PEMuxSel())

  val a = Wire(interconnectConfig.interPEType)
  val b = Wire(interconnectConfig.interPEType)
  val c = Wire(interconnectConfig.interPEType)
  val q = Wire(interconnectConfig.interPEType)

  val fu = Module(new FU(interconnectConfig))

  val double_buffer = Module(new DoubleBuffer(interconnectConfig))

  val PEMuxDict = Map(
    PEMuxSel.V_GRID -> io.in_v_grid,
    PEMuxSel.V -> io.in_v,
    PEMuxSel.H_GRID -> io.in_h_grid,
    PEMuxSel.H -> io.in_h,
    PEMuxSel.D -> io.in_d,
    PEMuxSel.REG -> double_buffer.io.r,
    PEMuxSel.ZERO -> interconnectConfig.interPEType.zero,
    PEMuxSel.IDENTITY -> interconnectConfig.interPEType.identity
  )

  io.out := Mux(io.control.sel_out === OutMuxSel.REG, double_buffer.io.s, fu.io.p)
  io.out_v_grid := Mux(io.control.sel_out_v_grid === OutGridMuxSel.FU, fu.io.p, io.in_v_grid)
  io.out_h_grid := Mux(io.control.sel_out_h_grid === OutGridMuxSel.FU, fu.io.p, io.in_h_grid)

  sel_a := io.control.sel_a
  sel_b := io.control.sel_b
  sel_c := io.control.sel_c
  sel_q := io.control.sel_q
  fu.io.control := io.control.fu_control

  fu.io.a := a
  fu.io.b := b
  fu.io.c := c
  double_buffer.io.p.bits := fu.io.p

  double_buffer.io.q.bits := q
  double_buffer.io.swap := io.control.double_buffer_swap
  double_buffer.io.q.valid := io.valid && io.control.reg_q_en
  double_buffer.io.p.valid := io.valid && io.control.reg_p_en

  // Multiplexer A
  val mux_a_mapping = interconnectConfig.allowed_sel_a.map(x => x.asUInt -> PEMuxDict(x))
  a := MuxLookup(sel_a.asUInt, mux_a_mapping.last._2, mux_a_mapping)

  // Multiplexer B
  val mux_b_mapping = interconnectConfig.allowed_sel_b.map(x => x.asUInt -> PEMuxDict(x))
  b := MuxLookup(sel_b.asUInt, mux_b_mapping.last._2, mux_b_mapping)

  // Multiplexer C
  val mux_c_mapping = interconnectConfig.allowed_sel_c.map(x => x.asUInt -> PEMuxDict(x))
  c := MuxLookup(sel_c.asUInt, mux_c_mapping.last._2, mux_c_mapping)

  // Multiplexer Q
  val mux_q_mapping = interconnectConfig.allowed_sel_q.map(x => x.asUInt -> PEMuxDict(x))
  q := MuxLookup(sel_q.asUInt, mux_q_mapping.last._2, mux_q_mapping)

}
