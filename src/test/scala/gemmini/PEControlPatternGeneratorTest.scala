package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._

class PEControlPatternGeneratorTest extends AnyFlatSpec with ChiselScalatestTester {
  "PEControlPatternGenerator" should "generate control patterns correctly" in {
    val interconnectConfig = CommonInterconnectConfigs.DefaultICConfig
    val sequenceTableSize = 4
    val controlPatternTableSize = 4

    val OS_control_set_COMP = (new ModPEControl(interconnectConfig)).Lit(
      _.sel_a -> PEMuxSel.H_GRID,
      _.sel_b -> PEMuxSel.V_GRID,
      _.sel_c -> PEMuxSel.REG,
      _.sel_q -> PEMuxSel.V,
      _.fu_control.big_alu_sel -> BigALUSel.MUL,
      _.fu_control.minus_a -> false.B,
      _.fu_control.minus_m -> false.B,
      _.fu_control.sel_m -> MMuxSel.BIG_ALU,
      _.fu_control.shift -> 0.U,
      _.fu_control.small_alu_sel -> SmallALUSel.ADD,
      _.sel_out_v_grid -> OutGridMuxSel.FORWARD,
      _.sel_out_h_grid -> OutGridMuxSel.FORWARD,
      _.sel_out -> OutMuxSel.REG,
      _.double_buffer_sel -> 0.U)

    val OS_control_set_PROP = (new ModPEControl(interconnectConfig)).Lit(
      _.sel_a -> PEMuxSel.H_GRID,
      _.sel_b -> PEMuxSel.V_GRID,
      _.sel_c -> PEMuxSel.REG,
      _.sel_q -> PEMuxSel.V,
      _.fu_control.big_alu_sel -> BigALUSel.MUL,
      _.fu_control.minus_a -> false.B,
      _.fu_control.minus_m -> false.B,
      _.fu_control.sel_m -> MMuxSel.BIG_ALU,
      _.fu_control.shift -> 0.U,
      _.fu_control.small_alu_sel -> SmallALUSel.ADD,
      _.sel_out_v_grid -> OutGridMuxSel.FORWARD,
      _.sel_out_h_grid -> OutGridMuxSel.FORWARD,
      _.sel_out -> OutMuxSel.REG,
      _.double_buffer_sel -> 1.U)

    test(new PEControlPatternGenerator(interconnectConfig, controlPatternTableSize)) { dut =>
        dut.io.valid.poke(false)
        dut.clock.step(1)

        dut.io.write_enable.poke(true.B)

        dut.io.write_index.poke(0)
        dut.io.write_data.pe_control.poke(OS_control_set_COMP)
        dut.io.write_data.repeat_for_n_cycles.poke(0)
        dut.io.write_data.next_index.poke(1)
        dut.clock.step(1)

        dut.io.write_index.poke(1)
        dut.io.write_data.pe_control.poke(OS_control_set_PROP)
        dut.io.write_data.repeat_for_n_cycles.poke(1)
        dut.io.write_data.next_index.poke(0)
        dut.clock.step(1)

        dut.io.write_index.poke(2)
        dut.io.write_data.pe_control.poke(OS_control_set_COMP)
        dut.io.write_data.repeat_for_n_cycles.poke(0)
        dut.io.write_data.next_index.poke(2)
        dut.clock.step(1)

        dut.io.write_enable.poke(false.B)

        dut.io.next_control_pattern_index.poke(1)
        dut.io.new_control_pattern.poke(true)
        dut.io.valid.poke(true)

        dut.clock.step(1)
        dut.io.new_control_pattern.poke(false)

        dut.io.pe_control.expect(OS_control_set_PROP)
        dut.clock.step(1)
        dut.io.pe_control.expect(OS_control_set_PROP)
        dut.clock.step(1)
        dut.io.pe_control.expect(OS_control_set_COMP)
        dut.clock.step(1)
        dut.io.pe_control.expect(OS_control_set_PROP)

        dut.io.next_control_pattern_index.poke(2)
        dut.io.new_control_pattern.poke(true)
        dut.clock.step(1)
        dut.io.new_control_pattern.poke(false)

        dut.io.pe_control.expect(OS_control_set_COMP)
        dut.clock.step(1)
        dut.io.pe_control.expect(OS_control_set_COMP)
        dut.clock.step(300)
        dut.io.pe_control.expect(OS_control_set_COMP)

        dut.io.next_control_pattern_index.poke(0)
        dut.io.new_control_pattern.poke(true)
        dut.clock.step(1)
        dut.io.new_control_pattern.poke(false)

        dut.io.pe_control.expect(OS_control_set_COMP)
        dut.clock.step(1)
        dut.io.pe_control.expect(OS_control_set_PROP)
        dut.clock.step(1)
        dut.io.pe_control.expect(OS_control_set_PROP)
        dut.clock.step(1)
        dut.io.pe_control.expect(OS_control_set_COMP)
    }
  }
}
