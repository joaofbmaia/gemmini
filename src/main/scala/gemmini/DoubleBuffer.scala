package gemmini

import chisel3._
import chisel3.util.RegEnable

class DoubleBuffer[T <: Data](interconnectConfig : InterconnectConfig[T]) extends Module {
  val io = IO(new Bundle {
    val p = Input(interconnectConfig.interPEType)
    val q = Input(interconnectConfig.interPEType)
    val r = Output(interconnectConfig.interPEType)
    val s = Output(interconnectConfig.interPEType)
    val sel = Input(UInt(1.W))
    val reg_enable = Input(Bool())
  })

  val A = Wire(interconnectConfig.interPEType)
  val B = Wire(interconnectConfig.interPEType)
  val regA = RegEnable(A, io.reg_enable)
  val regB = RegEnable(B, io.reg_enable)

  A := Mux(io.sel.asBool, io.q, io.p)
  B := Mux(io.sel.asBool, io.p, io.q)

  io.r := Mux(io.sel.asBool, regB, regA)
  io.s := Mux(io.sel.asBool, regA, regB)
}

// sel = 0 : P -> regA -> R ; Q -> regB -> S
// sel = 1 : P -> regB -> R ; Q -> regA -> S