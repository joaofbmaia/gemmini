package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import chisel3.stage.ChiselStage

class PETest extends AnyFlatSpec with ChiselScalatestTester{
  "Gemmini PE" should "pass" in {
    test(new PE(SInt(8.W), SInt(32.W), SInt(20.W), Dataflow.OS, 5)) { dut =>
    //   (new ChiselStage).emitVerilog(new PE(SInt(8.W), SInt(32.W), SInt(20.W), Dataflow.OS, 5), Array("--target-dir", "buildstuff"))
      dut.io.in_a.poke(69.S)
      dut.io.in_b.poke(420.S)
      dut.io.in_d.poke(37.S)
      dut.io.in_id.poke(1.U)
      dut.io.in_last.poke(false.B)
      dut.io.in_valid.poke(true.B)
      dut.io.in_control.dataflow.poke(Dataflow.OS.id.U(1.W))
      dut.io.in_control.propagate.poke(0.U)
      dut.io.in_control.shift.poke(0.U)

      dut.clock.step(1)
      println("c = " + dut.io.out_c.peekInt())
      dut.io.in_control.propagate.poke(1.U)
      dut.clock.step(1)


      println("a = " + dut.io.out_a.peekInt())
      println("b = " + dut.io.out_b.peekInt())
      println("c = " + dut.io.out_c.peekInt())
      println("PE PASS")
    }
  }
}
