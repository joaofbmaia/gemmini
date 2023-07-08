package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._

class PETest extends AnyFlatSpec with ChiselScalatestTester {
  "PE" should "load configuration patterns correctly" in {
    val interconnectConfig = CommonInterconnectConfigs.DefaultICConfig
    val sequenceTableSize = 4
    val controlPatternTableSize = 4

    println("Word width: " + interconnectConfig.interPEType.getWidth)
    println("Line width: " + (new ControlPatternTableLine(interconnectConfig, controlPatternTableSize)).getWidth)


    test(new PE(interconnectConfig, controlPatternTableSize)) { dut =>
        
    }
  }
}