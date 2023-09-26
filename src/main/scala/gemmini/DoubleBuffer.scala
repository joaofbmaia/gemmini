package gemmini

import chisel3._
import chisel3.util.RegEnable
import chisel3.util.Valid

class DoubleBuffer[T <: Data](interconnectConfig : InterconnectConfig[T]) extends Module {
  val io = IO(new Bundle {
    val p = Input(Valid(interconnectConfig.interPEType))
    val q = Input(Valid(interconnectConfig.interPEType))
    val r = Output(interconnectConfig.interPEType)
    val s = Output(interconnectConfig.interPEType)
    val swap = Input(Bool())
  })

  val selReg = Reg(Bool())
  selReg := selReg ^ io.swap

  val sel = Wire(Bool())
  sel := Mux(io.swap, selReg ^ io.swap, selReg)

  val A = Wire(Valid(interconnectConfig.interPEType))
  val B = Wire(Valid(interconnectConfig.interPEType))
  val regA = RegEnable(A.bits, A.valid)
  val regB = RegEnable(B.bits, B.valid)

  A := Mux(sel, io.q, io.p)
  B := Mux(sel, io.p, io.q)

  io.r := Mux(sel, regB, regA)
  io.s := Mux(sel, regA, regB)
}

// sel = 0 : P -> regA -> R ; Q -> regB -> S
// sel = 1 : P -> regB -> R ; Q -> regA -> S