package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._

class SequencingElementTest extends AnyFlatSpec with ChiselScalatestTester {
  "SequencingElement" should "generate configuration patterns indexes correctly" in {
    val sequenceTableSize = 4
    val controlPatternTableSize = 4

    test(new SequencingElement(sequenceTableSize, controlPatternTableSize)) { dut =>
        dut.io.fire_counter.poke(0)
        dut.io.sequencer_reset.poke(true.B)
        dut.clock.step(1)
        dut.io.sequencer_reset.poke(false.B)

        dut.io.write_enable.poke(true.B)
        for (i <- 0 until sequenceTableSize) {
            dut.io.write_index.poke(i)
            dut.io.write_data.start_after_cycle_number.poke(2 * i)
            dut.io.write_data.pattern_index.poke(i)
            dut.clock.step(1)
        }
        dut.io.write_enable.poke(false.B)
        dut.io.sequencer_reset.poke(true.B)
        dut.clock.step(1)
        dut.io.sequencer_reset.poke(false.B)
        // Cycle 0
        dut.io.fire_counter.poke(0)
        dut.io.next_control_pattern_index.expect(0)
        dut.io.new_control_pattern.expect(true)

        dut.clock.step(1)
        // Cycle 1
        dut.io.fire_counter.poke(1)
        dut.io.next_control_pattern_index.expect(1)
        dut.io.new_control_pattern.expect(false)
        
        dut.clock.step(1)
        // Cycle 2
        dut.io.fire_counter.poke(2)
        dut.io.next_control_pattern_index.expect(1)
        dut.io.new_control_pattern.expect(true)

        dut.clock.step(1)
        // Cycle 3
        dut.io.fire_counter.poke(3)
        dut.io.next_control_pattern_index.expect(2)
        dut.io.new_control_pattern.expect(false)

        dut.clock.step(1)
        // Cycle 4
        dut.io.fire_counter.poke(4)
        dut.io.next_control_pattern_index.expect(2)
        dut.io.new_control_pattern.expect(true)

        dut.clock.step(1)
        // Cycle 5
        dut.io.fire_counter.poke(5)
        dut.io.next_control_pattern_index.expect(3)
        dut.io.new_control_pattern.expect(false)

        dut.clock.step(1)
        // Cycle 6
        dut.io.fire_counter.poke(6)
        dut.io.next_control_pattern_index.expect(3)
        dut.io.new_control_pattern.expect(true)

        dut.clock.step(1)
        // Cycle 7
        dut.io.fire_counter.poke(7)
        dut.io.next_control_pattern_index.expect(0)
        dut.io.new_control_pattern.expect(false)

    }
  }
}
