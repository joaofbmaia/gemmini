// See README.md for license details.
package gemmini

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class ModPEControl[T <: Data : Arithmetic](interconnectConfig : InterconnectConfig[T]) extends Bundle {
  val sel_a = PEMuxSel()
  val sel_b = PEMuxSel()
  val sel_c = PEMuxSel()
  val fu_control = new FUControl(interconnectConfig)
}

object PEMuxSel extends ChiselEnum {
  val V_BCAST  = Value("b000".U)
  val V        = Value("b001".U)
  val H_BCAST  = Value("b010".U)
  val H        = Value("b011".U)
  val D        = Value("b100".U)
  val REG      = Value("b101".U)
  val ZERO     = Value("b110".U)
  val IDENTITY = Value("b111".U)
}

class ModPE[T <: Data](interconnectConfig : InterconnectConfig[T])
                   (implicit ev: Arithmetic[T]) extends Module { // Debugging variables
  import ev._

  val io = IO(new Bundle {
    val in_v_bcast = Input(interconnectConfig.verticalBroadcastType)
    val in_h_bcast = Input(interconnectConfig.horizontalBroadcastType)
    val out_v_bcast = Output(interconnectConfig.verticalBroadcastType)
    val out_h_bcast = Output(interconnectConfig.horizontalBroadcastType)
    val in_v = Input(interconnectConfig.interPEType)
    val in_h = Input(interconnectConfig.interPEType)
    val in_d = Input(interconnectConfig.interPEType)
    val out = Output(interconnectConfig.interPEType)

    // just for testing (CU not implemented yet)
    val control = Input(new ModPEControl(interconnectConfig))

    // daqui para baixo é SUS

    // val in_control = Input(new PEControl(interconnectConfig.interPEType))
    // val out_control = Output(new PEControl(interconnectConfig.interPEType))

    // val in_id = Input(UInt(log2Up(max_simultaneous_matmuls).W))
    // val out_id = Output(UInt(log2Up(max_simultaneous_matmuls).W))

    // val in_last = Input(Bool())
    // val out_last = Output(Bool())

    // val in_valid = Input(Bool())
    // val out_valid = Output(Bool())

    // val bad_dataflow = Output(Bool())
  })

  val out_reg = Reg(interconnectConfig.interPEType)

  val sel_a = Wire(PEMuxSel())
  val sel_b = Wire(PEMuxSel())
  val sel_c = Wire(PEMuxSel())

  val a = Wire(interconnectConfig.interPEType)
  val b = Wire(interconnectConfig.interPEType)
  val c = Wire(interconnectConfig.interPEType)

  val fu = Module(new FU(interconnectConfig))

  io.out := out_reg
  io.out_v_bcast := io.in_v_bcast
  io.out_h_bcast := io.in_h_bcast

  sel_a := io.control.sel_a
  sel_b := io.control.sel_b
  sel_c := io.control.sel_c
  fu.io.control := io.control.fu_control

  fu.io.a := a
  fu.io.b := b
  fu.io.c := c
  out_reg := fu.io.p

  // Begin Multiplexer A
  // default for mux_a
  a := interconnectConfig.interPEType.identity

  switch (sel_a) {
    // is (PEMuxSel.V_BCAST) {
    //   a := io.in_v_bcast
    // }
    // is (PEMuxSel.V) {
    //   a := io.in_v
    // }
    is (PEMuxSel.H_BCAST) {
      a := io.in_h_bcast
    }
    is (PEMuxSel.H) {
      a := io.in_h
    }
    is (PEMuxSel.D) {
      a := io.in_d
    }
    // is (PEMuxSel.REG) {
    //   a := out_reg
    // }
    is (PEMuxSel.ZERO) {
      a := interconnectConfig.interPEType.zero
    }
    is (PEMuxSel.IDENTITY) {
      a := interconnectConfig.interPEType.identity
    }
  }
  // End Multiplexer A


  // Begin Multiplexer B
  // default for mux_b
  b := interconnectConfig.interPEType.identity

  switch (sel_b) {
    is (PEMuxSel.V_BCAST) {
      b := io.in_v_bcast
    }
    is (PEMuxSel.V) {
      b := io.in_v
    }
    // is (PEMuxSel.H_BCAST) {
    //   b := io.in_h_bcast
    // }
    // is (PEMuxSel.H) {
    //   b := io.in_h
    // }
    // is (PEMuxSel.D) {
    //   b := io.in_d
    // }
    is (PEMuxSel.REG) {
      b := out_reg
    }
    is (PEMuxSel.ZERO) {
      b := interconnectConfig.interPEType.zero
    }
    is (PEMuxSel.IDENTITY) {
      b := interconnectConfig.interPEType.identity
    }
  }
  // End Multiplexer B


  // Begin Multiplexer C
  // default for mux_c
  c := interconnectConfig.interPEType.identity

  switch (sel_c) {
    // is (PEMuxSel.V_BCAST) {
    //   c := io.in_v_bcast
    // }
    is (PEMuxSel.V) {
      c := io.in_v
    }
    // is (PEMuxSel.H_BCAST) {
    //   c := io.in_h_bcast
    // }
    is (PEMuxSel.H) {
      c := io.in_h
    }
    is (PEMuxSel.D) {
      c := io.in_d
    }
    is (PEMuxSel.REG) {
      c := out_reg
    }
    is (PEMuxSel.ZERO) {
      c := interconnectConfig.interPEType.zero
    }
    is (PEMuxSel.IDENTITY) {
      c := interconnectConfig.interPEType.identity
    }
  }
  // End Multiplexer C

}
