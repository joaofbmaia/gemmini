package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import chisel3.stage.ChiselStage

class StreamResizerTest extends AnyFlatSpec with ChiselScalatestTester {
  "StreamResizer" should "upsize streams correctly" in {
    (new ChiselStage).emitVerilog(StreamResizer(32, 37))
    test(StreamResizer(4, 10)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        println(dut.ratio)
        dut.io.first.poke(true)
        dut.io.in.valid.poke(true)
        dut.io.in.bits.poke(1)
        dut.clock.step(1)
        dut.io.first.poke(false)
        dut.io.in.bits.poke(2)
        dut.clock.step(1)
        dut.io.in.bits.poke(3)
        println(dut.io.out.valid.peek())
        println(dut.io.out.bits.peek())
        dut.clock.step(1)

        dut.io.in.bits.poke(3)
        dut.clock.step(1)
        dut.io.in.bits.poke(2)
        dut.clock.step(1)
        dut.io.in.bits.poke(1)
        println(dut.io.out.valid.peek())
        println(dut.io.out.bits.peek())
        dut.clock.step(1)
        
        dut.io.in.bits.poke(3)
        dut.clock.step(1)
        dut.io.in.bits.poke(1)
        dut.io.in.valid.poke(false)
        dut.clock.step(1)
        dut.io.in.valid.poke(true)
        dut.io.in.bits.poke(2)
        dut.clock.step(1)
        
        
        dut.io.first.poke(true)
        dut.io.in.valid.poke(true)
        dut.io.in.bits.poke(1)
        dut.clock.step(1)
        dut.io.first.poke(false)
        dut.io.in.bits.poke(2)
        dut.clock.step(1)
        dut.io.in.bits.poke(3)
        println(dut.io.out.valid.peek())
        println(dut.io.out.bits.peek())
        dut.clock.step(1)
    }
  }
}
