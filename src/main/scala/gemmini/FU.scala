package gemmini

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object BigALUSel extends ChiselEnum {
  val MUL, ADD, SUB = Value
}

object SmallALUSel extends ChiselEnum {
  val ADD, SUB, MAX3, MIN3, EQ3, NEQ3 = Value
}

object MMuxSel extends ChiselEnum {
  val A, BIG_ALU = Value
}

class FUControl[T <: Data : Arithmetic](interconnectConfig : InterconnectConfig[T]) extends Bundle {
    val big_alu_sel = BigALUSel()
    val small_alu_sel = SmallALUSel()
    val minus_a = Bool()
    val minus_m = Bool()
    val sel_m = MMuxSel()
    val shift = UInt(log2Up(interconnectConfig.interPEType.getWidth).W) // TODO this isn't correct for Floats
}

class FU[T <: Data](interconnectConfig : InterconnectConfig[T])(implicit ev: Arithmetic[T]) extends Module {
    import ev._
    val io = IO(new Bundle {
        val a = Input(interconnectConfig.interPEType)
        val b = Input(interconnectConfig.interPEType)
        val c = Input(interconnectConfig.interPEType)
        val p = Output(interconnectConfig.interPEType)
        val control = Input(new FUControl(interconnectConfig))
    })

    val big_alu = Wire(interconnectConfig.interPEType)
    val small_alu = Wire(interconnectConfig.interPEType)
    val m = Wire(interconnectConfig.interPEType)

    val Ma = Wire(interconnectConfig.interPEType)
    val Mm = Wire(interconnectConfig.interPEType)

    val z = Wire(Bool())
    val n = Wire(Bool())

    z := (Ma === interconnectConfig.interPEType.zero) // a - b = 0
    n := (io.b > Ma) // a - b < 0

    when(io.control.minus_a) {
        Ma := interconnectConfig.interPEType.zero - io.a
    } .otherwise {
        Ma := io.a
    }

    when(io.control.minus_m) {
        Mm := interconnectConfig.interPEType.zero - m
    } .otherwise {
        Mm := m
    }

    big_alu := Ma * io.b // default
    switch (io.control.big_alu_sel) {
        is (BigALUSel.MUL) {
            big_alu := Ma * io.b
        }
        is (BigALUSel.ADD) {
            big_alu := Ma + io.b
        }
        is (BigALUSel.SUB) {
            big_alu := Ma - io.b
        }
    }

    m := big_alu //default
    switch (io.control.sel_m) {
        is (MMuxSel.BIG_ALU) {
            m := big_alu
        }
        is (MMuxSel.A) {
            m := io.a
        }
    }

    small_alu := Mm + io.c // default
    switch (io.control.small_alu_sel) {
        is (SmallALUSel.ADD) {
            small_alu := Mm + io.c
        }
        is (SmallALUSel.SUB) {
            small_alu := Mm - io.c
        }
        is (SmallALUSel.MAX3) {
            small_alu := Mux(~n & ~z, Mm, io.c)
        }
        is (SmallALUSel.MIN3) {
            small_alu := Mux(n, Mm, io.c)
        }
        is (SmallALUSel.EQ3) {
            small_alu := Mux(z, Mm, io.c)
        }
        is (SmallALUSel.NEQ3) {
            small_alu := Mux(~z, Mm, io.c)
        }
    }

    io.p := (small_alu >> io.control.shift)
}