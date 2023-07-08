package gemmini

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._

class PEControlSystemTest extends AnyFlatSpec with ChiselScalatestTester {
  "SequencingElement and PEControlPatternGenerator" should "generate control patterns correctly" in {
    val interconnectConfig = CommonInterconnectConfigs.DefaultICConfig
    val sequenceTableSize = 4
    val controlPatternTableSize = 4

    val OS_control_set_COMP = (new ModPEControl(interconnectConfig)).Lit(
      _.sel_a -> PEMuxSel.H_BCAST,
      _.sel_b -> PEMuxSel.V_BCAST,
      _.sel_c -> PEMuxSel.REG,
      _.sel_q -> PEMuxSel.V,
      _.fu_control.big_alu_sel -> BigALUSel.MUL,
      _.fu_control.minus_a -> false.B,
      _.fu_control.minus_m -> false.B,
      _.fu_control.sel_m -> MMuxSel.BIG_ALU,
      _.fu_control.shift -> 0.U,
      _.fu_control.small_alu_sel -> SmallALUSel.ADD,
      _.use_double_buffer -> true.B,
      _.double_buffer_sel -> 0.U)

    val OS_control_set_PROP = (new ModPEControl(interconnectConfig)).Lit(
      _.sel_a -> PEMuxSel.H_BCAST,
      _.sel_b -> PEMuxSel.V_BCAST,
      _.sel_c -> PEMuxSel.REG,
      _.sel_q -> PEMuxSel.V,
      _.fu_control.big_alu_sel -> BigALUSel.MUL,
      _.fu_control.minus_a -> false.B,
      _.fu_control.minus_m -> false.B,
      _.fu_control.sel_m -> MMuxSel.BIG_ALU,
      _.fu_control.shift -> 0.U,
      _.fu_control.small_alu_sel -> SmallALUSel.ADD,
      _.use_double_buffer -> true.B,
      _.double_buffer_sel -> 1.U)

    val garbage_control_set = (new ModPEControl(interconnectConfig)).Lit(
      _.sel_a -> PEMuxSel.H_BCAST,
      _.sel_b -> PEMuxSel.V_BCAST,
      _.sel_c -> PEMuxSel.REG,
      _.sel_q -> PEMuxSel.V,
      _.fu_control.big_alu_sel -> BigALUSel.MUL,
      _.fu_control.minus_a -> false.B,
      _.fu_control.minus_m -> false.B,
      _.fu_control.sel_m -> MMuxSel.BIG_ALU,
      _.fu_control.shift -> 0.U,
      _.fu_control.small_alu_sel -> SmallALUSel.ADD,
      _.use_double_buffer -> false.B,
      _.double_buffer_sel -> 0.U)


    class PEControlSystemWrapper[T <: Data](interconnectConfig : InterconnectConfig[T], sequenceTableSize: Int, controlPatternTableSize: Int) extends Module {
      val io = IO(new Bundle {
        val cycle_fire = Input(Bool())
        val sequencer_reset = Input(Bool())

        val pe_control = Output(new ModPEControl(interconnectConfig))

        // reconfiguration
        val sequencer_write_enable = Input(Bool())
        val sequencer_write_row = Input(UInt(1.W))
        val sequencer_write_column = Input(UInt(1.W))
        val sequencer_write_index_element = Input(UInt(log2Ceil(sequenceTableSize).W))
        val sequencer_write_data = Input(new SequenceTableLine(controlPatternTableSize))

        val CPG_write_enable = Input(Bool())
        val CPG_write_index = Input(UInt(log2Ceil(controlPatternTableSize).W))
        val CPG_write_data = Input(new ControlPatternTableLine(interconnectConfig, controlPatternTableSize))
      })

      val sequencer = Module(new Sequencer(interconnectConfig, sequenceTableSize, controlPatternTableSize, 1, 1))
      val cpg = Module(new PEControlPatternGenerator(interconnectConfig, controlPatternTableSize))
    
      sequencer.io.cycle_fire := io.cycle_fire
      sequencer.io.sequencer_reset := io.sequencer_reset

      cpg.io.valid := io.cycle_fire
      io.pe_control := cpg.io.pe_control

      cpg.io.new_control_pattern := sequencer.io.new_control_pattern(0)(0)
      cpg.io.next_control_pattern_index := sequencer.io.next_control_pattern_indexes(0)(0)


      sequencer.io.write_enable := io.sequencer_write_enable
      sequencer.io.write_row := io.sequencer_write_row
      sequencer.io.write_column := io.sequencer_write_column
      sequencer.io.write_index_element := io.sequencer_write_index_element
      sequencer.io.write_data := io.sequencer_write_data

      cpg.io.write_enable := io.CPG_write_enable
      cpg.io.write_index := io.CPG_write_index
      cpg.io.write_data := io.CPG_write_data
    
    }

    
    test(new PEControlSystemWrapper(interconnectConfig, sequenceTableSize, controlPatternTableSize)) { dut =>
        dut.io.cycle_fire.poke(false)
        dut.io.sequencer_reset.poke(true.B)
        dut.clock.step(1)
        dut.io.sequencer_reset.poke(false.B)
        // lets write to the sequencer memory
        dut.io.sequencer_write_enable.poke(true)
        dut.io.sequencer_write_row.poke(0)
        dut.io.sequencer_write_column.poke(0)
        // write line 0
        dut.io.sequencer_write_index_element.poke(0)
        dut.io.sequencer_write_data.start_after_cycle_number.poke(0)
        dut.io.sequencer_write_data.pattern_index.poke(0)
        dut.clock.step(1)
        // write line 1
        dut.io.sequencer_write_index_element.poke(1)
        dut.io.sequencer_write_data.start_after_cycle_number.poke(10)
        dut.io.sequencer_write_data.pattern_index.poke(1)
        dut.clock.step(1)
        // write line 2
        dut.io.sequencer_write_index_element.poke(2)
        dut.io.sequencer_write_data.start_after_cycle_number.poke(20)
        dut.io.sequencer_write_data.pattern_index.poke(0)
        dut.clock.step(1)
        // sequencer is programmed
        dut.io.sequencer_write_enable.poke(false)
        // lets write to the cgp memory
        dut.io.CPG_write_enable.poke(true)
        // write line 0
        dut.io.CPG_write_index.poke(0)
        dut.io.CPG_write_data.pe_control.poke(garbage_control_set)
        dut.io.CPG_write_data.repeat_for_n_cycles.poke(0)
        dut.io.CPG_write_data.next_index.poke(0)
        dut.clock.step(1)
        // write line 1
        dut.io.CPG_write_index.poke(1)
        dut.io.CPG_write_data.pe_control.poke(OS_control_set_PROP)
        dut.io.CPG_write_data.repeat_for_n_cycles.poke(1)
        dut.io.CPG_write_data.next_index.poke(2)
        dut.clock.step(1)
        // write line 2
        dut.io.CPG_write_index.poke(2)
        dut.io.CPG_write_data.pe_control.poke(OS_control_set_COMP)
        dut.io.CPG_write_data.repeat_for_n_cycles.poke(1)
        dut.io.CPG_write_data.next_index.poke(1)
        dut.clock.step(1)
        // cgp is programmed
        dut.io.CPG_write_enable.poke(false)
        // lets reset
        dut.io.cycle_fire.poke(true)
        dut.io.sequencer_reset.poke(true.B)
        dut.clock.step(1)
        dut.io.sequencer_reset.poke(false.B)
        // extra clock cycle to update cgo registers
        dut.clock.step(1)

        for(i <- 1 to 10) {
          dut.io.pe_control.expect(garbage_control_set)
          dut.clock.step(1)
        }
        for(i <- 1 to 2) {
          dut.io.pe_control.expect(OS_control_set_PROP)
          dut.clock.step(1)
        }
        for(i <- 1 to 2) {
          dut.io.pe_control.expect(OS_control_set_COMP)
          dut.clock.step(1)
        }
        dut.io.cycle_fire.poke(false)
        // random 3 cycle no fire
        dut.clock.step(3)
        dut.io.cycle_fire.poke(true)
        for(i <- 1 to 2) {
          dut.io.pe_control.expect(OS_control_set_PROP)
          dut.clock.step(1)
        }
        for(i <- 1 to 2) {
          dut.io.pe_control.expect(OS_control_set_COMP)
          dut.clock.step(1)
        }
        for(i <- 1 to 2) {
          dut.io.pe_control.expect(OS_control_set_PROP)
          dut.clock.step(1)
        }
        dut.io.pe_control.expect(garbage_control_set)
        dut.clock.step(1)
    }
  }
}
