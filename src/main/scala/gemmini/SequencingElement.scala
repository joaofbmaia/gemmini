package gemmini

import chisel3._
import chisel3.util._

class SequenceTableLine(controlPatternTableSize: Int) extends Bundle {
  val start_after_cycle_number = UInt(32.W)
  val pattern_index = UInt(log2Ceil(controlPatternTableSize).W)
}

class SequencingElement(sequenceTableSize: Int, controlPatternTableSize: Int) extends Module { 
  val io = IO(new Bundle {
    val fire_counter = Input(UInt(32.W))
    val sequencer_reset = Input(Bool())
    
    // which configuration pattern to use
    val next_control_pattern_index = Output(UInt(log2Ceil(controlPatternTableSize).W))

    // true when the configuration pattern to use changes
    val new_control_pattern = Output(Bool())


    // reconfiguration
    val write_enable = Input(Bool())
    val write_index = Input(UInt(log2Ceil(sequenceTableSize).W))
    val write_data = Input(new SequenceTableLine(controlPatternTableSize))
  })


  val new_pattern = Wire(Bool())

  // sequence table index
  val sequence_table_index = RegInit(0.U(log2Ceil(sequenceTableSize).W))
  when(io.sequencer_reset) {
    sequence_table_index := 0.U
  }.elsewhen(new_pattern) {
    sequence_table_index := sequence_table_index + 1.U
  }

  // Sequence Table
  val sequence_table = Mem(sequenceTableSize, new SequenceTableLine(controlPatternTableSize))
  val next_pattern = sequence_table(sequence_table_index)

  // Comparator
  new_pattern := next_pattern.start_after_cycle_number === io.fire_counter  && !io.write_enable

  // Outputs
  io.new_control_pattern := new_pattern
  io.next_control_pattern_index := next_pattern.pattern_index

  // Reconfiguration
  when(io.write_enable) {
    sequence_table(io.write_index) := io.write_data
  }
}
