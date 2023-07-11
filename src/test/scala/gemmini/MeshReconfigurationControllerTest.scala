package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import chisel3.stage.ChiselStage

class MeshReconfigurationControllerTest extends AnyFlatSpec with ChiselScalatestTester {
  "MeshReconfigurationController" should "generate correct reconfiguration signals" in {
    (new ChiselStage).emitVerilog(new MeshReconfigurationController(2, 2, 4, 4))
    test(new MeshReconfigurationController(2, 2, 4, 4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.io.start.poke(true)
        dut.clock.step(1)
        dut.io.start.poke(false)
        dut.clock.step(40)
    }
  }
}
